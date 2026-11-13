Optimism Shader - experimental 26.3 test build

Based on Optimism-shader by Nachtwind1:
https://modrinth.com/shader/optimism-shader
The original pack is MIT licensed; see LICENSE.txt. This modified version is
not an official release by the original author. The unmodified source archive
is preserved separately as shaders/optimism.zip in this repository.

Changes in this test build:
- Fix invalid integer-suffixed float literals rejected by GLSL compilers.
- Use GLSL 330 instead of requiring 460. A graphics driver supporting OpenGL
  3.3 and the requirements of Minecraft and Iris is still required.
- Replace an undefined reversed smoothstep, correct grayscale averaging, and
  initialize the sky's second output in both sky and star branches.

Installation: place optimism-26.3-test.zip in your Minecraft instance's
shaderpacks folder (do not unzip it). Use Iris and Sodium versions made for
Minecraft 26.3, then select the pack in Video Settings > Shader Packs.

Status: the shaders were checked offline with glslang, including vertex and
fragment linking. They have NOT been tested in Minecraft, Iris, or on Windows
7. Version 26.3 compatibility and performance are not yet confirmed. If the
pack fails, share the shader error and your instance's logs/latest.log. If it
renders incorrectly, share screenshots and say where and when it happens.
