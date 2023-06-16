#define XXH_STATIC_LINKING_ONLY   /* access advanced declarations */
#define XXH_IMPLEMENTATION   /* access definitions */

#include "xxhash.h"
#include "stddef.h"
#include "string.h"

uint64_t hash_bytes(const char *s, size_t len) {
    return XXH64(s, len, 0x2a47eba5d9afb4efull);
}

uint64_t hash2(uint64_t a, uint64_t b) {
    if (!(a & b)) {
        uint64_t val = a | b;
        return hash_bytes((const char *)&val, sizeof(uint64_t));
    } else {
        return XXH64_mergeRound(a, b);
    }
}

uint64_t sc_hash (uint64_t data, size_t size) {
    return hash_bytes((const char *)&data, (size > 8)?8:size);
}

uint64_t sc_hash2x64(uint64_t a, uint64_t b) {
    return hash2(a, b);
}

uint64_t sc_hashbytes (const char *data, size_t size) {
    return hash_bytes(data, size);
}
