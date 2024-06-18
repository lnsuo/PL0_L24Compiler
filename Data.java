public class Data {
    String vs = "";
    int vn = 0;
    private boolean isNum = false;

    public Data() {
        vn = 0;
        isNum = true;
    }

    public Data(int n) {
        vn = n;
        isNum = true;
    }

    public Data(String s) {
        vs = s;
        isNum = false;
    } 

    public boolean isNum() {
        return isNum;
    }

    public void change(int n) {
        vn = n;
        isNum = true;
    }

    public void change(String s) {
        vs = s;
        isNum = false;
    }

    public Data clone() {
        if (isNum) {
            return new Data(vn);
        } else {
            return new Data(vs);
        }
    }

    public String toString() {
        if (isNum) {
            return Integer.toString(vn);
        } else {
            return vs;
        }
    }
}
