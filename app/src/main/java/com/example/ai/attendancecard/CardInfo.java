package com.example.ai.attendancecard;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chenwanfeng on 2018/4/16.
 */

public class CardInfo implements Parcelable {
    //判断是否在公司
    public boolean inCompany;
    // 自动获取的打卡信息: WIFI/定位地址
    public String msg;
    // 考勤状态 文字: 正常上班打卡 0 / 正常下班打卡 1 /迟到 2 /早退 3
    public int state;

    public CardInfo(boolean inCompany, String msg) {
        this(inCompany, msg, -1);
    }

    public CardInfo(boolean inCompany, String msg, int state) {
        this.msg = msg;
        this.inCompany = inCompany;
        this.state = state;
    }

    protected CardInfo(Parcel in) {
        inCompany = in.readByte() != 0;
        this.msg = in.readString();
        this.state = in.readInt();
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
        dest.writeInt(this.state);
    }

    public String getStateStr(int state) {
        String str = "未知";
        switch (state) {
            case 0:
                str = "上班打卡";
                break;
            case 1:
                str = "下班打卡";
                break;
            case 2:
                str = "迟到";
                break;
            case 3:
                str = "早退";
                break;
        }
        return str;
    }
}
