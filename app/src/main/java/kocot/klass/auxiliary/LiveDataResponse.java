package kocot.klass.auxiliary;

public class LiveDataResponse {

    private boolean successful;
    private String info;

    private Object data;
    private boolean hasData;

    public LiveDataResponse(){

        successful = false;
        hasData = false;
        data = null;
        info = "Unknown error occurred";

    }

    public LiveDataResponse(boolean successful, String info){

        this.successful = successful;
        this.info = info;

    }

    public LiveDataResponse(boolean successful, String info, Object data){

        this.successful = successful;
        this.info = info;

        if(data != null){

            this.hasData = true;
            this.data = data;

        } else {

            this.hasData = false;

        }


    }

    public boolean isSuccessful() {
        return successful;
    }


    public String getInfo() {
        return info;
    }

    public boolean hasData(){

        return hasData;

    }

    public Object getData(){

        return data;

    }


    public void setResponse(boolean successful, String info){

        this.successful = successful;

        if(!successful && info == null){
            this.info = " Failed ";
            return;
        } else if(successful && info == null){
            this.info = " ";
            return;
        }

        this.info = info;

    }
}
