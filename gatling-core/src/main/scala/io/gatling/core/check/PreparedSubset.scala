/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.check

/**
 * Declares which part of the prepared value a check reads. When all the checks sharing the same Preparer declare one, the preparation can be restricted to the
 * union of these subsets, eg only build the parts of a JSON tree the checks need. A check that doesn't declare any needs the whole prepared value.
 *
 * This is only a hint: a check must return the same result when given the whole prepared value.
 */
trait PreparedSubset
