public class Rectangle implements Shape {
    private final String id;
    private final double length;
    private final double breadth;

    public Rectangle(String id, double length, double breadth) {
        this.id = id;
        this.length = length;
        this.breadth = breadth;
    }

    @Override
    public double getArea() {
        return Math.round(length * breadth);
    }

    @Override
    public double getPerimeter() {
        return Math.round(2.0 * (length + breadth));
    }

    @Override
    public String toString() {
        return id + "=>" + getArea() + ":" + getPerimeter();
    }
}
