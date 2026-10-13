const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const {compileCollision, collisionCopy, stripCollisionGeometry} = require('./oritech_collision.js');

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
        Blockbench: blockbench, Cube: {preview_controller: preview, prototype: {menu: {addAction() {}}}}, Codecs: {bedrock: codec},
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

test('copies dimensions, baking inflation and stretch without modifying visual cubes', () => {
    const source = cube({inflate: 1, stretch: [2, 1, 1], visibility: false, export: false});
    const before = JSON.stringify(source);
    assert.deepEqual(collisionCopy(source), {name: 'box_collision', from: [-18, -1, -9], to: [18, 17, 9]});
    assert.equal(JSON.stringify(source), before);
});

test('bakes cube and ancestor quarter turns around their pivots', () => {
    const source = cube({from: [0, 0, 0], to: [2, 4, 6], origin: [1, 2, 0], rotation: [0, 0, 90],
        parent: group([], {name: 'visual', rotation: [0, 90, 0], origin: [0, 0, 0], parent: 'root'})});
    assert.deepEqual(collisionCopy(source), {name: 'box_collision', from: [0, 1, -3], to: [6, 3, 1]});
    assert.throws(() => collisionCopy(cube({parent: group([], {rotation: [0, 45, 0]})})), /axis-aligned/);
    assert.throws(() => collisionCopy(cube({rotation: [30, 0, 0]})), /axis-aligned/);
    assert.throws(() => collisionCopy(cube({inflate: -8})), /positive/);
});

test('context action copies a selection in one undo edit and rejects invalid batches before editing', () => {
    let plugin, undo, edits = 0, message;
    const roots = [];
    const actions = {};
    const menu = [];
    class MockCube {
        static selected = [];
        static preview_controller = emitter();
        constructor(data) { Object.assign(this, cube(data)); }
        addTo(parent) { this.parent = parent; parent.children.push(this); return this; }
        init() { return this; }
        selectLow() { MockCube.selected.push(this); }
    }
    MockCube.prototype.menu = {addAction(action) { menu.push(action); }};
    const context = {
        Plugin: {register(id, data) { plugin = data; }},
        Format: {id: 'geckolib_model'}, Modes: {edit: {selected: true}}, Outliner: {root: roots},
        Cube: MockCube,
        Group: class {
            constructor(data) { Object.assign(this, group([], data)); }
            addTo(parent) { assert.equal(parent, 'root'); roots.push(this); return this; }
            init() { return this; }
        },
        Undo: {initEdit(data) { undo = data; edits++; }, finishEdit() { assert.equal(undo.elements.length, 2); }},
        unselectAllElements() { MockCube.selected = []; }, updateSelection() {},
        Blockbench: {...emitter(), showMessageBox(data) { message = data.message; }},
        Codecs: {bedrock: emitter()}, MenuBar: {addAction() {}},
        Action: class {constructor(id, data) { Object.assign(this, data); actions[id] = this; } delete() {}},
        THREE: {MeshBasicMaterial: class {dispose() {}}},
    };
    vm.runInNewContext(fs.readFileSync(require.resolve('./oritech_collision.js'), 'utf8'), context);
    plugin.onload();
    const action = actions.add_as_oritech_collision_box;
    assert.equal(menu[0], action);
    assert.equal(action.condition(), false);
    const sources = [cube(), cube({name: 'upper', from: [0, 16, 0], to: [8, 24, 8]})];
    MockCube.selected = sources.slice();
    assert.equal(action.condition(), true);
    action.click();
    assert.equal(edits, 1);
    assert.equal(roots.length, 1);
    assert.equal(roots[0].name, 'collision');
    assert.equal(roots[0].children.length, 2);
    assert.equal(MockCube.selected.length, 2);
    assert.ok(MockCube.selected.every(copy => !sources.includes(copy)));
    assert.equal(undo.selection, true);
    assert.equal(compileCollision(roots).boxes.length, 2);

    // Reuse the existing root, and leave it untouched if any selected cube is invalid.
    MockCube.selected = [sources[0], cube({rotation: [0, 45, 0]})];
    action.click();
    assert.match(message, /axis-aligned/);
    assert.equal(edits, 1);
    assert.equal(roots[0].children.length, 2);
    MockCube.selected = sources.slice();
    action.click();
    assert.equal(roots.length, 1);
    assert.equal(roots[0].children.length, 4);
    context.Modes.edit.selected = false;
    assert.equal(action.condition(), false);
    plugin.onunload();
});
