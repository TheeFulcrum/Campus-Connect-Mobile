import bpy
import math
import os
from pathlib import Path
from mathutils import Vector

# ---------- Scene ----------
scene = bpy.context.scene
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 1024
scene.render.resolution_y = 1024
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = "PNG"
scene.render.image_settings.color_mode = "RGBA"
scene.render.film_transparent = True

script_text = bpy.data.texts.get("campus_connect_logo.py")
if script_text and script_text.filepath:
    output_dir = Path(os.path.dirname(bpy.path.abspath(script_text.filepath)))
else:
    output_dir = Path("/home/methembe-nyathi/Videos/CampusConnectMobile/blender")
output_dir.mkdir(parents=True, exist_ok=True)
scene.render.filepath = str(output_dir / "campus_connect_logo.png")

# Remove the default scene.
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

# ---------- Materials ----------
def make_material(name, color, metallic=0.0, roughness=0.38):
    material = bpy.data.materials.new(name)
    material.diffuse_color = (*color, 1.0)
    material.use_nodes = True
    principled = material.node_tree.nodes.get("Principled BSDF")
    principled.inputs["Base Color"].default_value = (*color, 1.0)
    principled.inputs["Metallic"].default_value = metallic
    principled.inputs["Roughness"].default_value = roughness
    return material

orange = make_material("CampusConnect Orange", (1.0, 0.4196, 0.2706), roughness=0.32)


def apply_material(obj, material=orange):
    obj.data.materials.append(material)


def bevel(obj, amount=0.04, segments=3):
    modifier = obj.modifiers.new("Soft logo edges", "BEVEL")
    modifier.width = amount
    modifier.segments = segments
    modifier.limit_method = "ANGLE"

# ---------- Geometry helpers ----------
def create_prism(name, vertices_2d, depth=0.16, z=0.0):
    """Create a filled 2D polygon extruded along Z."""
    mesh = bpy.data.meshes.new(name + " Mesh")
    half_depth = depth / 2.0
    vertices = [(x, y, z - half_depth) for x, y in vertices_2d]
    vertices += [(x, y, z + half_depth) for x, y in vertices_2d]
    count = len(vertices_2d)
    faces = []
    faces.append(tuple(range(count - 1, -1, -1)))
    faces.append(tuple(range(count, count * 2)))
    for index in range(count):
        next_index = (index + 1) % count
        faces.append((index, next_index, count + next_index, count + index))
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    apply_material(obj)
    bevel(obj, 0.035, 3)
    return obj


def create_cylinder_between(name, start, end, radius=0.055, material=orange):
    start = Vector(start)
    end = Vector(end)
    direction = end - start
    length = direction.length
    midpoint = (start + end) / 2.0
    bpy.ops.mesh.primitive_cylinder_add(
        vertices=32,
        radius=radius,
        depth=length,
        location=midpoint,
    )
    obj = bpy.context.object
    obj.name = name
    obj.rotation_mode = "QUATERNION"
    obj.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(direction)
    apply_material(obj, material)
    bevel(obj, radius * 0.35, 3)
    return obj


def create_node(name, location, radius=0.18):
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=32,
        ring_count=16,
        radius=radius,
        location=location,
    )
    obj = bpy.context.object
    obj.name = name
    apply_material(obj)
    return obj

# ---------- Logo layout ----------
# The model lies mostly in the XY plane, with thickness along Z.
cap_top = [(-3.5, 1.35), (0.0, 3.35), (3.5, 1.35), (0.0, -0.65)]
create_prism("Mortarboard Top", cap_top, depth=0.22)

cap_band = [(-2.05, 0.05), (0.0, -1.10), (2.05, 0.05), (2.05, -0.55), (0.0, -1.72), (-2.05, -0.55)]
create_prism("Flattened Skull Cap", cap_band, depth=0.20, z=-0.02)

# Network hub and outer nodes.
hub = (0.0, -1.10, 0.18)
left = (-1.91, -2.42, 0.18)
bottom = (0.0, -3.00, 0.18)
right = (1.91, -2.42, 0.18)

create_cylinder_between("Left Network Link", hub, left, 0.105)
create_cylinder_between("Bottom Network Link", hub, bottom, 0.105)
create_cylinder_between("Right Network Link", hub, right, 0.105)

create_node("Central Hub", hub, 0.46)
create_node("Left Node", left, 0.36)
create_node("Bottom Node", bottom, 0.36)
create_node("Right Node", right, 0.36)

# Tassel on the right side of the cap.
tassel_anchor = (2.80, 1.35, 0.18)
tassel_end = (2.80, 0.05, 0.18)
create_cylinder_between("Tassel String", tassel_anchor, tassel_end, 0.065)
create_node("Tassel Knot", tassel_end, 0.14)

for index in range(7):
    x = 2.62 + index * 0.06
    create_cylinder_between(
        "Tassel Strand %02d" % (index + 1),
        (x, -0.08, 0.18),
        (x, -0.95, 0.18),
        0.018,
    )

# ---------- Camera ----------
bpy.ops.object.camera_add(location=(0.0, -0.15, 16.0))
camera = bpy.context.object
camera.name = "Logo Camera"
camera.data.type = "ORTHO"
camera.data.ortho_scale = 9.5
camera.rotation_euler = (0.0, 0.0, 0.0)
# Point the camera down the negative Z axis.
camera.rotation_euler = (0.0, 0.0, 0.0)
scene.camera = camera

# ---------- Lighting ----------
bpy.ops.object.light_add(type="AREA", location=(-4.5, 4.5, 7.0))
key_light = bpy.context.object
key_light.name = "Key Light"
key_light.data.energy = 700
key_light.data.shape = "DISK"
key_light.data.size = 5.0
key_light.rotation_euler = (math.radians(22), 0.0, math.radians(-35))

bpy.ops.object.light_add(type="AREA", location=(4.5, -2.0, 5.0))
fill_light = bpy.context.object
fill_light.name = "Fill Light"
fill_light.data.energy = 350
fill_light.data.size = 4.0
fill_light.rotation_euler = (math.radians(28), 0.0, math.radians(140))

# World background is transparent for Android integration.
scene.world.color = (0.025, 0.04, 0.08)

# Select the logo objects so the result is easy to inspect after running.
bpy.ops.object.select_all(action="DESELECT")
for obj in bpy.context.scene.objects:
    if obj.type == "MESH":
        obj.select_set(True)

# Save the .blend file beside the script and render the transparent PNG.
bpy.ops.wm.save_as_mainfile(filepath=str(output_dir / "campus_connect_logo.blend"))
bpy.ops.render.render(write_still=True)
print("CampusConnect logo created and rendered to: " + scene.render.filepath)
