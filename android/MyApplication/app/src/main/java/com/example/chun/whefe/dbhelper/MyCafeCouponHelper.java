package com.example.chun.whefe.dbhelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Chun on 2017-05-30.
 */

public class MyCafeCouponHelper extends SQLiteOpenHelper {
    public MyCafeCouponHelper(Context context) {
        super(context, "cafe_coupon.sqlite", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cafecoupon (coupon_num INTEGER, coupon_name VARCHAR(20), cafe_id VARCHAR(20), coupon_price VARCHAR(20), coupon_start VARCHAR(20), coupon_end VARCHAR(20), use_ox Boolean);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("CREATE TABLE cafecoupon (coupon_num INTEGER, coupon_name VARCHAR(20), cafe_id VARCHAR(20), coupon_price VARCHAR(20), coupon_start VARCHAR(20), coupon_end VARCHAR(20), use_ox Boolean);");

    }
}
