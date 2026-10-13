const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const {compileCollision, stripCollisionGeometry} = require('./oritech_collision.js');

const cube = (extra = {}) => ({type: 'cube', name: 'box', from: [-8, 0, -8], to: [8, 16, 8], ...extra});
const group = (children, extra = {}) => ({type: 'group', name: 'collision', children, ...extra});

test('exports only root collision descendants, in controller-local block units', () => {
    const roots = [cube(), group([cube()], {name: 'visual'}), group([
        cube(), group([cube({from: [8, 16, -24], to: [24, 32, -8]})], {name: 'upper'})
    ])];
    assert.deepEqual(compileCollision(roots), {
        format_version: 1,
        coordinate_system: 'oritech:block_local_north',
        boxes: [{min: [0, 0, 0], max: [1, 1, 1]}, {min: [1, 1, -1], max: [2, 2, 0]}]
    });
});

test('visibility, UVs and normal export flags do not determine collision membership', () => {
    const data = compileCollision([group([cube({visibility: false, export: false, inflate: 1})], {export: false})]);
    assert.deepEqual(data.boxes[0], {min: [-0.0625, -0.0625, -0.0625], max: [1.0625, 1.0625, 1.0625]});
});

test('rejects ambiguous roots, unsupported transforms and invalid geometry', () => {
    for (const roots of [[], [group([])], [group([cube()]), group([cube()])],
        [group([group([cube()])], {name: 'visual'})],
        [group([cube({rotation: [0, 45, 0]})])],
        [group([cube()], {rotation: [0, 90, 0]})],
        [group([cube({stretch: [2, 1, 1]})])],
        [group([cube({to: [-8, 16, 8]})])],
        [group([cube({from: [NaN, 0, 0]})])],
        [group([cube({inflate: Infinity})])],
        [group([{type: 'mesh'}])]]) {
        assert.throws(() => compileCollision(roots));
    }
});

test('normal geometry export removes collision descendants even out of order', () => {
    const model = {'minecraft:geometry': [{bones: [
        {name: 'deep', parent: 'child'}, {name: 'visual'},
        {name: 'collision'}, {name: 'child', parent: 'collision'}
    ]}, {bones: [{name: 'visual'}, {name: 'collision', parent: 'visual'}]}]};
    stripCollisionGeometry(model);
    assert.deepEqual(model['minecraft:geometry'][0].bones, [{name: 'visual'}]);
    assert.equal(model['minecraft:geometry'][1].bones.length, 2);
});

test('source bone names exclude children when the codec omits a non-exporting root', () => {
    const model = {'minecraft:geometry': [{bones: [{name: 'child', parent: 'collision'}, {name: 'visual'}]}]};
    stripCollisionGeometry(model, ['collision', 'child']);
    assert.deepEqual(model['minecraft:geometry'][0].bones, [{name: 'visual'}]);
});

function emitter() {
    const events = new Map();
    return {
        on(name, fn) { events.set(name, fn); },
        removeListener(name, fn) { assert.equal(events.get(name), fn); events.delete(name); },
        fire(name, data) { events.get(name)?.(data); },
        events,
    };
}

test('plugin exports, restores preview state, and cleans up listeners/materials', () => {
    let plugin;
    let exported;
    const actions = {};
    const blockbench = {...emitter(), export(data) { exported = data; }, showMessageBox(data) { throw new Error(data.message); }};
    const preview = emitter();
    const codec = emitter();
    const original = {};
    const mesh = {material: original, renderOrder: 3};
    const roots = [group([cube({mesh})])];
    const context = {
        Plugin: {register(id, data) { plugin = data; }},
        Format: {id: 'geckolib_model'}, Outliner: {root: roots}, Project: {name: 'assembler'},
        Blockbench: blockbench, Cube: {preview_controller: preview}, Codecs: {bedrock: codec},
        MenuBar: {addAction() {}},
        Action: class { constructor(id, data) { Object.assign(this, data); actions[id] = this; } delete() { this.deleted = true; } },
        THREE: {DoubleSide: 2, MeshBasicMaterial: class {
            constructor(options) { Object.assign(this, options); } dispose() { this.disposed = true; }
        }},
    };
    vm.runInNewContext(fs.readFileSync(require.resolve('./oritech_collision.js'), 'utf8'), context);
    plugin.onload();
    const overlay = mesh.material;
    assert.equal(overlay.depthTest, false);
    assert.equal(overlay.depthWrite, false);
    assert.equal(overlay.transparent, true);
    assert.equal(mesh.renderOrder, 10000);
    actions.export_oritech_collision.click();
    assert.equal(exported.name, 'assembler.collision.json');
    assert.deepEqual(JSON.parse(exported.content), compileCollision(roots));
    // Material refreshes from Blockbench must not destroy the override or restoration target.
    const refreshed = {};
    mesh.material = refreshed;
    preview.fire('update_faces');
    assert.equal(mesh.material, overlay);
    roots[0].name = 'visual';
    blockbench.fire('render_frame');
    assert.equal(mesh.material, refreshed);
    assert.equal(mesh.renderOrder, 3);
    roots[0].name = 'collision';
    blockbench.fire('render_frame');
    context.Format.id = 'java_block';
    blockbench.fire('render_frame');
    assert.equal(mesh.material, refreshed);
    context.Format.id = 'geckolib_model';
    blockbench.fire('render_frame');
    plugin.onunload();
    assert.equal(mesh.material, refreshed);
    assert.equal(mesh.renderOrder, 3);
    assert.equal(overlay.disposed, true);
    assert.equal(blockbench.events.size + preview.events.size + codec.events.size, 0);
    assert.ok(Object.values(actions).every(action => action.deleted));
});
