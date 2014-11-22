// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package org.uncommons.maths.random;

import java.util.Random;
import org.uncommons.maths.binary.BinaryUtils;

/**
 * <p>Very fast pseudo random number generator.  See
 * <a href="http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html">this
 * page</a> for a description.  This RNG has a period of about 2^160, which is not as long
 * as the {@link MersenneTwisterRNG} but it is faster.</p>
 *
 * <p><em>NOTE: Because instances of this class require 160-bit seeds, it is not
 * possible to seed this RNG using the {@link #setSeed(long)} method inherited
 * from {@link Random}.  Calls to this method will have no effect.
 * Instead the seed must be set by a constructor.</em></p>
 *
 * @author Daniel Dyer
 * @since 1.2
 */
// edit: removed ReentrantLock as we use this inside a ThreadLocalRandom
public class UnsafeXORShiftRNG extends Random implements RepeatableRNG
{
  private static final int SEED_SIZE_BYTES = 20; // Needs 5 32-bit integers.

  // Previously used an array for state but using separate fields proved to be
  // faster.
  private int state1;
  private int state2;
  private int state3;
  private int state4;
  private int state5;

  private final byte[] seed;


  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public UnsafeXORShiftRNG()
  {
    this(DefaultSeedGenerator.getInstance().generateSeed(SEED_SIZE_BYTES));
  }


  /**
   * Seed the RNG using the provided seed generation strategy.
   * @param seedGenerator The seed generation strategy that will provide
   * the seed value for this RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  public UnsafeXORShiftRNG(SeedGenerator seedGenerator) throws SeedException
  {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }


  /**
   * Creates an RNG and seeds it with the specified seed data.
   * @param seed The seed data used to initialise the RNG.
   */
  public UnsafeXORShiftRNG(byte[] seed)
  {
    if (seed == null || seed.length != SEED_SIZE_BYTES)
    {
      throw new IllegalArgumentException("XOR shift RNG requires 160 bits of seed data.");
    }
    this.seed = seed.clone();
    int[] state = BinaryUtils.convertBytesToInts(seed);
    this.state1 = state[0];
    this.state2 = state[1];
    this.state3 = state[2];
    this.state4 = state[3];
    this.state5 = state[4];
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
  protected int next(int bits)
  {
    int t = (state1 ^ (state1 >> 7));
    state1 = state2;
    state2 = state3;
    state3 = state4;
    state4 = state5;
    state5 = (state5 ^ (state5 << 6)) ^ (t ^ (t << 13));
    int value = (state2 + state2 + 1) * state5;
    return value >>> (32 - bits);
  }
}