/*
 * Copyright (C) 2005-2016 UCWeb Inc. All rights reserved.
 *  Description :MultipleBarcodeReader.java
 *
 *  Creation    : 2016-12-10
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.google.zxing.multi;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;

import java.util.Map;

/**
 * Implementation of this interface attempt to read several barcodes from one image.
 *
 * @see com.google.zxing.Reader
 * @author Sean Owen
 */
public interface MultipleBarcodeReader {

  Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException;

  Result[] decodeMultiple(BinaryBitmap image,
                          Map<DecodeHintType, ?> hints) throws NotFoundException;

}
