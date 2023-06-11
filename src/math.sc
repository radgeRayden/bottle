using import glm

do
    inline orthographic-projection (width height)
        # https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/orthographic-projection-matrix
        # right, top
        let r t = (width / 2) (height / 2)
        # left, bottom
        let l b = -r -t
        # far, near
        let f n = 100 -100
        mat4
            vec4 (2 / (r - l), 0.0, 0.0, -((r + l) / (r - l)))
            vec4 (0.0, 2 / (t - b), 0.0, 0.0)
            vec4 (-((t + b) / (t - b)), 0.0, -2 / (f - n), -((f + n) / (f - n)))
            vec4 0.0 0.0 0.0 1.0

    inline... translation-matrix (v : vec3)
        mat4
            vec4 1   0   0   0
            vec4 0   1   0   0
            vec4 0   0   1   0
            vec4 v.x v.y v.z 1

    inline... scaling-matrix (v : vec3)
        mat4
            vec4 v.x   0    0   0
            vec4 0    v.y   0   0
            vec4 0     0   v.z  0
            vec4 0     0    0   1

    local-scope;
