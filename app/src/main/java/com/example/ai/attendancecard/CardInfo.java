package com.example.ai.attendancecard;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chenwanfeng on 2018/4/16.
 */

public class CardInfo implements Parcelable {
    public boolean inCompany;
    public String msg;

    public CardInfo(boolean inCompany ,String msg) {
        this.msg = msg;
        this.inCompany = inCompany;
    }

    protected CardInfo(Parcel in) {
        inCompany = in.readByte() != 0;
        this.msg = in.readString();
    }

    public static final Creator<CardInfo> CREATOR = new Creator<CardInfo>() {
        @Override
        public CardInfo createFromParcel(Parcel in) {
            return new CardInfo(in);
        }

        @Override
        public CardInfo[] newArray(int size) {
            return new CardInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (inCompany ? 1 : 0));
        dest.writeString(this.msg);
    }
}
