package org.uncommons.maths.random;


import java.util.Random;
import org.uncommons.maths.binary.BinaryUtils;

/**
 * <p>Random number generator based on the
 * <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html" target="_top">Mersenne
 * Twister</a> algorithm developed by Makoto Matsumoto and Takuji Nishimura.</p>
 *
 * <p>This is a very fast random number generator with good statistical
 * properties (it passes the full DIEHARD suite).  This is the best RNG
 * for most experiments.  If a non-linear generator is required, use
 * the slower {@link AESCounterRNG} RNG.</p>
 *
 * <p>This PRNG is deterministic, which can be advantageous for testing purposes
 * since the output is repeatable.  If multiple instances of this class are created
 * with the same seed they will all have identical output.</p>
 *
 * <p>This code is translated from the original C version and assumes that we
 * will always seed from an array of bytes.  I don't pretend to know the
 * meanings of the magic numbers or how it works, it just does.</p>
 *
 * <p><em>NOTE: Because instances of this class require 128-bit seeds, it is not
 * possible to seed this RNG using the {@link #setSeed(long)} method inherited
 * from {@link Random}.  Calls to this method will have no effect.
 * Instead the seed must be set by a constructor.</em></p>
 *
 * @author Makoto Matsumoto and Takuji Nishimura (original C version)
 * @author Daniel Dyer (Java port)
 * @author Stéphane Landelle (fork that removes ReentrantLock based thread safety)
 */
public class UnsafeMersenneTwisterRNG extends Random implements RepeatableRNG
{
  // The actual seed size isn't that important, but it should be a multiple of 4.
  private static final int SEED_SIZE_BYTES = 16;

  // Magic numbers from original C version.
  private static final int N = 624;
  private static final int M = 397;
  private static final int[] MAG01 = {0, 0x9908b0df};
  private static final int UPPER_MASK = 0x80000000;
  private static final int LOWER_MASK = 0x7fffffff;
  private static final int BOOTSTRAP_SEED = 19650218;
  private static final int BOOTSTRAP_FACTOR = 1812433253;
  private static final int SEED_FACTOR1 = 1664525;
  private static final int SEED_FACTOR2 = 1566083941;
  private static final int GENERATE_MASK1 = 0x9d2c5680;
  private static final int GENERATE_MASK2 = 0xefc60000;

  private final byte[] seed;

  private final int[] mt = new int[N]; // State vector.
  private int mtIndex = 0; // Index into state vector.


  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public UnsafeMersenneTwisterRNG()
  {
    this(DefaultSeedGenerator.getInstance().generateSeed(SEED_SIZE_BYTES));
  }


  /**
   * Seed the RNG using the provided seed generation strategy.
   * @param seedGenerator The seed generation strategy that will provide
   * the seed value for this RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  public UnsafeMersenneTwisterRNG(SeedGenerator seedGenerator) throws SeedException
  {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }



  /**
   * Creates an RNG and seeds it with the specified seed data.
   * @param seed The seed data used to initialise the RNG.
   */
  public UnsafeMersenneTwisterRNG(byte[] seed)
  {
    if (seed == null || seed.length != SEED_SIZE_BYTES)
    {
      throw new IllegalArgumentException("Mersenne Twister RNG requires a 128-bit (16-byte) seed.");
    }
    this.seed = seed.clone();

    int[] seedInts = BinaryUtils.convertBytesToInts(this.seed);

    // This section is translated from the init_genrand code in the C version.
    mt[0] = BOOTSTRAP_SEED;
    for (mtIndex = 1; mtIndex < N; mtIndex++)
    {
      mt[mtIndex] = (BOOTSTRAP_FACTOR
        * (mt[mtIndex - 1] ^ (mt[mtIndex - 1] >>> 30))
        + mtIndex);
    }

    // This section is translated from the init_by_array code in the C version.
    int i = 1;
    int j = 0;
    for (int k = Math.max(N, seedInts.length); k > 0; k--)
    {
      mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * SEED_FACTOR1)) + seedInts[j] + j;
      i++;
      j++;
      if (i >= N)
      {
        mt[0] = mt[N - 1];
        i = 1;
      }
      if (j >= seedInts.length)
      {
        j = 0;
      }
    }
    for (int k = N - 1; k > 0; k--)
    {
      mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * SEED_FACTOR2)) - i;
      i++;
      if (i >= N)
      {
        mt[0] = mt[N - 1];
        i = 1;
      }
    }
    mt[0] = UPPER_MASK; // Most significant bit is 1 - guarantees non-zero initial array.
  }


  /**
   * {@inheritDoc}
   */
  public byte[] getSeed()
  {
    return seed.clone();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected final int next(int bits)
  {
    int y;
    if (mtIndex >= N) // Generate N ints at a time.
    {
      int kk;
      for (kk = 0; kk < N - M; kk++)
      {
        y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
        mt[kk] = mt[kk + M] ^ (y >>> 1) ^ MAG01[y & 0x1];
      }
      for (; kk < N - 1; kk++)
      {
        y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
        mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ MAG01[y & 0x1];
      }
      y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
      mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ MAG01[y & 0x1];

      mtIndex = 0;
    }

    y = mt[mtIndex++];
    // Tempering
    y ^= (y >>> 11);
    y ^= (y << 7) & GENERATE_MASK1;
    y ^= (y << 15) & GENERATE_MASK2;
    y ^= (y >>> 18);

    return y >>> (32 - bits);
  }
}
