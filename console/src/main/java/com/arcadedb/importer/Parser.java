/*
 * Copyright 2021 Arcade Data Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.arcadedb.importer;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

public class Parser {
  private final Source            source;
  private final InputStream       is;
  private final InputStreamReader reader;
  private final long              limit;
  private       AtomicLong        position = new AtomicLong();
  private       long              total;
  private       char              currentChar;
  private       boolean           compressed;

  public Parser(final Source source, final long limit) throws IOException {
    this.source = source;
    this.is = new BufferedInputStream(source.inputStream) {
      @Override
      public int read() throws IOException {
        position.incrementAndGet();
        return super.read();
      }

      @Override
      public int read(final byte[] b) throws IOException {
        if (limit > 0 && position.get() > limit)
          throw new EOFException();

        final int res = super.read(b);
        position.addAndGet(res);
        return res;
      }

      @Override
      public int read(final byte[] b, final int off, final int len) throws IOException {
        if (limit > 0 && position.get() > limit)
          throw new EOFException();

        int res = super.read(b, off, len);
        position.addAndGet(res);
        return res;
      }

      @Override
      public int available() throws IOException {
        if (limit > 0 && position.get() > limit)
          return 0;

        return super.available();
      }

      @Override
      public synchronized void reset() throws IOException {
        pos = 0;
        position.set(0);
      }
    };

    this.compressed = source.compressed;
    this.total = source.totalSize;

    this.reader = new InputStreamReader(this.is);
    this.limit = limit;
    this.is.mark(0);
  }

  public char getCurrentChar() {
    return currentChar;
  }

  public char nextChar() throws IOException {
    position.incrementAndGet();
    currentChar = (char) reader.read();
    return currentChar;
  }

  public void mark() {
    is.mark(0);
  }

  public void reset() throws IOException {
    currentChar = 0;
    position.set(0);
    is.reset();
  }

  public boolean isAvailable() throws IOException {
    if (limit > 0)
      return position.get() < limit && is.available() > 0;
    return is.available() > 0;
  }

  public InputStream getInputStream() {
    return is;
  }

  public long getPosition() {
    return position.get();
  }

  public long getTotal() {
    return limit > 0 ? Math.min(limit, total) : total;
  }

  public boolean isCompressed() {
    return compressed;
  }

  public Source getSource() {
    return source;
  }
}
