package com.app.builder.core.security

/**
 * Hashes [blocks] with MurmurHash3 x64 128 and a zero seed, reading each pair of longs as one 16-byte block.
 * MurmurHash3 is fast and spreads its input evenly, but it is not cryptographic: it identifies data, it does not protect it.
 *
 * @param blocks The longs to hash, an even number of them.
 * @return The two halves of the 128-bit hash.
 */
internal fun murmur3(blocks: LongArray): Pair<Long, Long> {
    val c1 = 0x87c37b91114253d5uL.toLong()
    val c2 = 0x4cf5ad432745937fL
    var h1 = 0L
    var h2 = 0L
    for (i in blocks.indices step 2) {
        h1 = h1 xor (blocks[i] * c1).rotateLeft(bitCount = 31) * c2
        h1 = (h1.rotateLeft(bitCount = 27) + h2) * 5 + 0x52dce729
        h2 = h2 xor (blocks[i + 1] * c2).rotateLeft(bitCount = 33) * c1
        h2 = (h2.rotateLeft(bitCount = 31) + h1) * 5 + 0x38495ab5
    }
    val length = blocks.size * Long.SIZE_BYTES.toLong()
    h1 = h1 xor length
    h2 = h2 xor length
    h1 += h2
    h2 += h1
    h1 = fmix(k = h1)
    h2 = fmix(k = h2)
    h1 += h2
    h2 += h1
    return h1 to h2
}

/**
 * Applies the MurmurHash3 64-bit finalization mix, so every input bit affects every output bit.
 *
 * @param k The value to mix.
 * @return The mixed value.
 */
private fun fmix(k: Long): Long {
    var mixed = k
    mixed = (mixed xor (mixed ushr 33)) * 0xff51afd7ed558ccduL.toLong()
    mixed = (mixed xor (mixed ushr 33)) * 0xc4ceb9fe1a85ec53uL.toLong()
    return mixed xor (mixed ushr 33)
}
