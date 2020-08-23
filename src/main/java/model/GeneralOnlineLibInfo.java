package model;

import model.libInfo.OnlineLibInfo;
import model.libInfo.TransLibInfo;

public class GeneralOnlineLibInfo {
    private OnlineLibInfo onlineLibInfo;
    private TransLibInfo transLibInfo;
    public GeneralOnlineLibInfo(){

    }
    public GeneralOnlineLibInfo(OnlineLibInfo onlineLibInfo){
        this.onlineLibInfo = onlineLibInfo;
    }

    public GeneralOnlineLibInfo(OnlineLibInfo onlineLibInfo, TransLibInfo transLibInfo){
        this.onlineLibInfo = onlineLibInfo;
        this.transLibInfo = transLibInfo;
    }

    public OnlineLibInfo getOnlineLibInfo() {
        return onlineLibInfo;
    }

    public void setOnlineLibInfo(OnlineLibInfo onlineLibInfo) {
        this.onlineLibInfo = onlineLibInfo;
    }

    public void setTransLibInfo(TransLibInfo transLibInfo){
        this.transLibInfo = transLibInfo;
    }

    public TransLibInfo getTransLibInfo() {
        return transLibInfo;
    }
}
