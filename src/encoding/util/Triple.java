package encoding.util;

public class Triple<T, U, X> {
    private T first;
    private U second;
    private X third;

    public Triple(){}

    public Triple(T first, U second, X third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }


    public void setFirst(T first) {
        this.first = first;
    }

    public T getFirst() {
        return first;
    }

    public void setSecond(U second) {
        this.second = second;
    }

    public U getSecond() {
        return second;
    }

    public void setThird(X third) {
        this.third = third;
    }

    public X getThird() {
        return third;
    }

    @Override
    public String toString() {
        return "first: " + this.first + " second : " + this.second + " third: " + this.third;
    }
}
