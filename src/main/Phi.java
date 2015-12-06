package main;

public class Phi
{
    private double value;
    
    public Phi(double value) {
        this.value = value;
    }
    
    public double getValue() {
        return value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }
    
    public void addToValue(double addition) {
        this.value += addition;
    }
}
