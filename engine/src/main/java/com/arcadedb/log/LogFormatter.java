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

package com.arcadedb.log;

import com.arcadedb.utility.AnsiCode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Basic Log formatter.
 *
 * @author Luca Garulli
 */

public class LogFormatter extends Formatter {

  protected static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * The end-of-line character for this platform.
   */
  protected static final String EOL = System.getProperty("line.separator");

  @Override
  public String format(final LogRecord record) {
    if (record.getThrown() == null) {
      return customFormatMessage(record);
    }

    // FORMAT THE STACK TRACE
    final StringBuilder buffer = new StringBuilder(512);
    buffer.append(record.getMessage());

    final Throwable current = record.getThrown();
    if (current != null) {
      buffer.append(EOL);

      StringWriter writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);

      current.printStackTrace(printWriter);
      printWriter.flush();

      buffer.append(writer.getBuffer());
      printWriter.close();
    }

    return buffer.toString();
  }

  protected String customFormatMessage(final LogRecord iRecord) {
    final Level level = iRecord.getLevel();
    final String message = AnsiCode.format(iRecord.getMessage(), false);
    final Object[] additionalArgs = iRecord.getParameters();
    final String requester = getSourceClassSimpleName(iRecord.getLoggerName());

    final StringBuilder buffer = new StringBuilder(512);
    buffer.append(EOL);
    synchronized (dateFormat) {
      buffer.append(dateFormat.format(new Date()));
    }

    buffer.append(String.format(" %-5.5s ", level.getName()));

    if (requester != null) {
      buffer.append("[");
      buffer.append(requester);
      buffer.append("] ");
    }

    // FORMAT THE MESSAGE
    try {
      if (additionalArgs != null)
        buffer.append(String.format(message, additionalArgs));
      else
        buffer.append(message);
    } catch (IllegalFormatException ignore) {
      buffer.append(message);
    }

    return AnsiCode.format(buffer.toString(), false);
  }

  protected String getSourceClassSimpleName(final String iSourceClassName) {
    if (iSourceClassName == null)
      return null;
    return iSourceClassName.substring(iSourceClassName.lastIndexOf(".") + 1);
  }
}
