/*
 *
 *  *  Copyright 2021 Arcade Data Ltd
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: https://arcadedb.com
 *
 */
package com.arcadedb.query.sql.parser.operators;

import com.arcadedb.query.sql.executor.QueryHelper;
import com.arcadedb.query.sql.parser.LikeOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Luigi Dell'Aquila (luigi.dellaquila-(at)-gmail.com)
 */
public class OLikeOperatorTest {
  @Test
  public void test() {
    LikeOperator op = new LikeOperator(-1);
    Assertions.assertTrue(op.execute(null, "foobar", "%ooba%"));
    Assertions.assertTrue(op.execute(null, "foobar", "%oo%"));
    Assertions.assertFalse(op.execute(null, "foobar", "oo%"));
    Assertions.assertFalse(op.execute(null, "foobar", "%oo"));
    Assertions.assertFalse(op.execute(null, "foobar", "%fff%"));
    Assertions.assertTrue(op.execute(null, "foobar", "foobar"));
  }

  @Test
  public void replaceSpecialCharacters() {
    Assertions.assertEquals("\\\\\\[\\]\\{\\}\\(\\)\\|\\*\\+\\$\\^\\...*", QueryHelper.convertForRegExp("\\[]{}()|*+$^.?%"));
  }
}
