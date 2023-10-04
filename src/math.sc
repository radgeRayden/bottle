using import glm

do
    let ceil =
        (extern 'llvm.ceil.f32 (function f32 f32))

    fn... orthographic-projection (width : f32, height : f32, far : f32, near : f32)
        # right, top
        r t := (width / 2), (height / 2)
        # left, bottom
        l b := -r, -t
        f n := far, near

        mat4
            vec4 (2 / (r - l), 0.0, 0.0, -((r + l) / (r - l)))
            vec4 (0.0, 2 / (t - b), 0.0, 0.0)
            vec4 (-((t + b) / (t - b)), 0.0, -2 / (f - n), -((f + n) / (f - n)))
            vec4 (0.0, 0.0, 0.0, 1.0)

    case (width : i32, height : i32)
        this-function (f32 width) (f32 height) 100:f32 -100:f32

    fn... perspective-projection (width : f32, height : f32, hFOV : f32, near : f32)
        aspect := height / width
        # https://discourse.nphysics.org/t/reversed-z-and-infinite-zfar-in-projections/341/2
        f := 1.0 / (tan (0.5 * hFOV))
        p00 := f
        p11 := f / aspect
        p22 := -1.0
        p23 := -near
        p32 := -1.0

        mat4
            vec4 (p00, 0.0, 0.0, 0.0)
            vec4 (0.0, p11, 0.0, 0.0)
            vec4 (0.0, 0.0, p22, p23)
            vec4 (0.0, 0.0, p32, 0.0)

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


    inline... rotate2D (v : vec2, angle : f32)
        let rcos rsin = (cos angle) (sin angle)
        vec2
            (rcos * v.x) - (rsin * v.y)
            (rsin * v.x) + (rcos * v.y)

    local-scope;
