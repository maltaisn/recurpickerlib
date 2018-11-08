/*
 * Copyright (c) 2018 Nicolas Maltais
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.maltaisn.recurpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

/**
 * This could have been done by XML on API >= 21 but before that, we can't define a drawable color
 * by referencing an attribute. So instead this drawable does the same thing programmatically.
 */
class WeekBtnDrawable extends StateListDrawable {

    WeekBtnDrawable(Context context) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{
                R.attr.rpColorWeekButtonChecked,
                R.attr.rpColorWeekButtonUnchecked,
                R.attr.rpColorWeekButtonStroke,
                R.attr.rpWeekButtonStrokeWidth,
                R.attr.rpWeekButtonSize,
        });
        int checkedColor = ta.getColor(0, 0);
        int uncheckedColor = ta.getColor(1, 0);
        int strokeColor = ta.getColor(2, 0);
        int strokeWidth = ta.getDimensionPixelSize(3, 0);
        int drawableSize = ta.getDimensionPixelSize(4, 0);
        ta.recycle();

        GradientDrawable checkedDrw = new GradientDrawable();
        checkedDrw.setShape(GradientDrawable.OVAL);
        checkedDrw.setStroke(strokeWidth, strokeColor);
        checkedDrw.setColor(checkedColor);
        checkedDrw.setSize(drawableSize, drawableSize);

        GradientDrawable uncheckedDrw = new GradientDrawable();
        uncheckedDrw.setShape(GradientDrawable.OVAL);
        uncheckedDrw.setStroke(strokeWidth, strokeColor);
        uncheckedDrw.setColor(uncheckedColor);
        uncheckedDrw.setSize(drawableSize, drawableSize);

        addState(new int[]{android.R.attr.state_checked}, checkedDrw);
        addState(new int[]{}, uncheckedDrw);
    }

}
