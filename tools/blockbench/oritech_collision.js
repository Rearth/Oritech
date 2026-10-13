/* Oritech collision authoring. Load this file with Blockbench's Load Plugin from File. */
(function () {
    'use strict';

    function collisionRoots(roots) {
        return roots.filter(node => node.type === 'group' && node.name === 'collision');
    }

    function collisionNodes(roots) {
        const nodes = new Set();
        function visit(node) {
            nodes.add(node);
            for (const child of node.children || []) visit(child);
        }
        collisionRoots(roots).forEach(visit);
        return nodes;
    }

    function vector(value, label) {
        if (!Array.isArray(value) || value.length !== 3 || !value.every(Number.isFinite)) {
            throw new Error(`${label} must contain three finite numbers.`);
        }
        return value;
    }

    function compileCollision(roots) {
        const groups = collisionRoots(roots);
        if (groups.length !== 1) throw new Error('Create exactly one root-level bone named "collision".');
        const boxes = [];
        for (const node of collisionNodes(roots)) {
            const label = node.name || node.type;
            if (vector(node.rotation || [0, 0, 0], `${label}: rotation`).some(n => n !== 0)) {
                throw new Error(`${label}: collision cubes and bones must be unrotated. Resize axis-aligned boxes instead.`);
            }
            if (node.type === 'group') continue;
            if (node.type !== 'cube') throw new Error(`${label}: only cubes and bones are allowed inside collision.`);
            if (vector(node.stretch || [1, 1, 1], `${label}: stretch`).some(n => n !== 1)) {
                throw new Error(`${label}: apply stretch to the cube dimensions before exporting.`);
            }
            const from = vector(node.from, `${label}: from`);
            const to = vector(node.to, `${label}: to`);
            const inflate = node.inflate ?? 0;
            if (!Number.isFinite(inflate)) throw new Error(`${label}: inflate must be finite.`);
            // Blockbench mirrors X when exporting Bedrock; GeckoLib mirrors it back.
            // Its block renderer then translates by (0.5, 0, 0.5).
            const offset = [0.5, 0, 0.5];
            const min = from.map((n, i) => (n - inflate) / 16 + offset[i]);
            const max = to.map((n, i) => (n + inflate) / 16 + offset[i]);
            if (!min.every((n, i) => Number.isFinite(n) && Number.isFinite(max[i]) && n < max[i])) {
                throw new Error(`${label}: each box dimension must be positive after inflation.`);
            }
            boxes.push({min, max});
        }
        if (!boxes.length) throw new Error('The collision bone contains no cubes.');
        return {format_version: 1, coordinate_system: 'oritech:block_local_north', boxes};
    }

    // Bake authored dimensions and quarter-turn rotations into an unrotated copy.
    // Bone pivots are model-space coordinates; apply the cube transform first, then its ancestors.
    function collisionCopy(source) {
        const from = vector(source.from, `${source.name}: from`);
        const to = vector(source.to, `${source.name}: to`);
        const stretch = vector(source.stretch || [1, 1, 1], `${source.name}: stretch`);
        const inflate = source.inflate ?? 0;
        if (!Number.isFinite(inflate)) throw new Error(`${source.name}: inflate must be finite.`);

        const center = from.map((n, i) => (n + to[i]) / 2);
        const half = from.map((n, i) => ((to[i] - n) / 2 + inflate) * stretch[i]);
        if (!half.every(n => Number.isFinite(n) && n > 0)) {
            throw new Error(`${source.name}: each box dimension must be positive.`);
        }
        const corners = Array.from({length: 8}, (_, corner) => center.map((n, i) => n + ((corner >> i) & 1 ? half[i] : -half[i])));

        for (let node = source; node && typeof node === 'object'; node = node.parent) {
            const rotation = vector(node.rotation || [0, 0, 0], `${node.name}: rotation`);
            if (rotation.some(angle => angle % 90 !== 0)) {
                throw new Error(`${node.name}: only axis-aligned cubes and bones (multiples of 90°) can be copied.`);
            }
            const pivot = vector(node.origin || [0, 0, 0], `${node.name}: pivot`);
            for (const point of corners) {
                for (let axis = 0; axis < 3; axis++) {
                    const a = (axis + 1) % 3;
                    const b = (axis + 2) % 3;
                    const turns = ((rotation[axis] / 90) % 4 + 4) % 4;
                    for (let turn = 0; turn < turns; turn++) {
                        const previousA = point[a] - pivot[a];
                        point[a] = pivot[a] - (point[b] - pivot[b]);
                        point[b] = pivot[b] + previousA;
                    }
                }
            }
        }
        return {
            name: `${source.name || 'box'}_collision`,
            from: [0, 1, 2].map(i => Math.min(...corners.map(point => point[i]))),
            to: [0, 1, 2].map(i => Math.max(...corners.map(point => point[i]))),
        };
    }

    // Remove only the exported root collision subtree, never modify the authoring project.
    function stripCollisionGeometry(model, sourceBoneNames = []) {
        for (const geometry of model['minecraft:geometry'] || []) {
            const bones = geometry.bones || [];
            const removed = new Set([
                ...sourceBoneNames,
                ...bones.filter(b => b && !b.parent && b.name === 'collision').map(b => b.name),
            ]);
            let changed;
            do {
                changed = false;
                for (const bone of bones) {
                    if (bone && removed.has(bone.parent) && !removed.has(bone.name)) {
                        removed.add(bone.name);
                        changed = true;
                    }
                }
            } while (changed);
            geometry.bones = bones.filter(b => b && !removed.has(b.name));
        }
    }

    // Same source is exercised by Node tests, without requiring a Blockbench installation.
    if (typeof Plugin === 'undefined') {
        if (typeof module !== 'undefined') module.exports = {compileCollision, collisionCopy, collisionNodes, stripCollisionGeometry};
        return;
    }

    let material;
    let exportAction;
    let addAction;
    let copyAction;
    const overridden = new Map();
    const isGecko = () => typeof Format !== 'undefined' && Format.id === 'geckolib_model';

    function restore(mesh, saved) {
        // Another renderer may have refreshed the material in the meantime.
        if (mesh.material === material) mesh.material = saved.material;
        mesh.renderOrder = saved.renderOrder;
    }

    function refreshPreview() {
        const nodes = isGecko() ? collisionNodes(Outliner.root) : new Set();
        const meshes = new Set();
        for (const node of nodes) {
            if (node.type !== 'cube' || !node.mesh) continue;
            const mesh = node.mesh;
            meshes.add(mesh);
            if (!overridden.has(mesh)) {
                overridden.set(mesh, {material: mesh.material, renderOrder: mesh.renderOrder});
            } else if (mesh.material !== material) {
                overridden.get(mesh).material = mesh.material;
            }
            mesh.material = material;
            mesh.renderOrder = 10000;
        }
        for (const [mesh, saved] of overridden) {
            if (!meshes.has(mesh)) {
                restore(mesh, saved);
                overridden.delete(mesh);
            }
        }
    }

    function onCompile(event) {
        if (isGecko()) {
            const names = [...collisionNodes(Outliner.root)].filter(node => node.type === 'group').map(node => node.name);
            stripCollisionGeometry(event.model, names);
        }
    }

    Plugin.register('oritech_collision', {
        title: 'Oritech Collision Shapes',
        author: 'Oritech',
        description: 'Author collision boxes with a through-model preview and export Oritech collision assets.',
        icon: 'select_all',
        version: '1.1.0',
        min_version: '4.12.0',
        variant: 'both',
        onload() {
            material = new THREE.MeshBasicMaterial({
                color: 0x38d9ff,
                transparent: true,
                opacity: 0.3,
                depthTest: false,
                depthWrite: false,
                side: THREE.DoubleSide,
            });
            exportAction = new Action('export_oritech_collision', {
                name: 'Export Oritech Collision Shapes',
                icon: 'save',
                condition: isGecko,
                click() {
                    try {
                        const data = compileCollision(Outliner.root);
                        const name = (Project.name || 'machine').replace(/(?:\.geo)?\.(?:json|bbmodel)$/, '');
                        Blockbench.export({
                            resource_id: 'oritech_collision',
                            type: 'Oritech Collision Shapes',
                            extensions: ['json'],
                            name: `${name}.collision.json`,
                            savetype: 'text',
                            content: JSON.stringify(data, null, 2) + '\n',
                        });
                    } catch (error) {
                        Blockbench.showMessageBox({title: 'Cannot export collision shapes', message: error.message});
                    }
                },
            });
            addAction = new Action('add_oritech_collision_box', {
                name: 'Add Oritech Collision Box',
                icon: 'add_box',
                condition: isGecko,
                click() {
                    const roots = collisionRoots(Outliner.root);
                    if (roots.length > 1) {
                        Blockbench.showMessageBox({title: 'Collision bones', message: 'Keep exactly one root-level collision bone.'});
                        return;
                    }
                    const elements = [];
                    Undo.initEdit({elements, outliner: true});
                    const root = roots[0] || new Group({name: 'collision', origin: [0, 0, 0]}).addTo('root').init();
                    const cube = new Cube({name: 'collision_box', from: [-8, 0, -8], to: [8, 16, 8]}).addTo(root).init();
                    elements.push(cube);
                    cube.select();
                    Undo.finishEdit('Add collision box');
                    refreshPreview();
                },
            });
            copyAction = new Action('add_as_oritech_collision_box', {
                name: 'Add as collision box',
                icon: 'content_copy',
                condition: () => isGecko() && Modes.edit.selected && Cube.selected.length > 0,
                click() {
                    let copies;
                    const roots = collisionRoots(Outliner.root);
                    try {
                        if (roots.length > 1) throw new Error('Keep exactly one root-level collision bone.');
                        if (roots[0] && vector(roots[0].rotation || [0, 0, 0], 'collision: rotation').some(n => n !== 0)) {
                            throw new Error('The root collision bone must be unrotated.');
                        }
                        // Validate the entire selection before starting an undo edit or creating anything.
                        copies = Cube.selected.map(collisionCopy);
                    } catch (error) {
                        Blockbench.showMessageBox({title: 'Cannot add collision boxes', message: error.message});
                        return;
                    }
                    if (!copies.length) return;

                    const elements = [];
                    Undo.initEdit({elements, outliner: true, selection: true});
                    const root = roots[0] || new Group({name: 'collision', origin: [0, 0, 0]}).addTo('root').init();
                    unselectAllElements();
                    for (const data of copies) {
                        // Fresh cubes keep all faces enabled and do not inherit textures or export/visibility flags.
                        const copy = new Cube(data).addTo(root).init();
                        elements.push(copy);
                        copy.selectLow();
                    }
                    updateSelection();
                    Undo.finishEdit('Add selection as collision boxes');
                    refreshPreview();
                },
            });
            Cube.prototype.menu.addAction(copyAction);
            MenuBar.addAction(exportAction, 'file.export');
            MenuBar.addAction(addAction, 'edit');
            Codecs.bedrock.on('compile', onCompile);
            Cube.preview_controller.on('update_faces', refreshPreview);
            Blockbench.on('render_frame', refreshPreview);
            refreshPreview();
        },
        onunload() {
            Blockbench.removeListener('render_frame', refreshPreview);
            Cube.preview_controller.removeListener('update_faces', refreshPreview);
            Codecs.bedrock.removeListener('compile', onCompile);
            for (const [mesh, saved] of overridden) restore(mesh, saved);
            overridden.clear();
            material.dispose();
            exportAction.delete();
            addAction.delete();
            copyAction.delete();
        },
    });
})();
