package com.tempoplatform.bl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebResponse {

    // 200
    public String status;
    public String id;
    public Double cpm;
    public String location_url_suffix;

    // 400
    public String message;
    public String error;

    // 422
    public Detail detail[];
    public class Detail {
        public JSONArray loc;
        public String msg;
        public String type;
    }

    public WebResponse(JSONObject jsonObject) throws JSONException {
        status = jsonObject.optString("status");
        id = jsonObject.optString("id");
        cpm = jsonObject.isNull("cpm") ? null : jsonObject.optDouble("cpm");

        message = jsonObject.optString("message");
        error = jsonObject.optString("error");

        JSONArray detailArray = jsonObject.optJSONArray("detail");
        if (detailArray != null) {
            detail = new Detail[detailArray.length()];
            for (int i = 0; i < detailArray.length(); i++) {
                JSONObject detailObject = detailArray.getJSONObject(i);
                Detail detailItem = new Detail();
                detailItem.loc = detailObject.optJSONArray("loc");
                detailItem.msg = detailObject.optString("msg");
                detailItem.type = detailObject.optString("type");
                detail[i] = detailItem;
            }
        }
    }
}
