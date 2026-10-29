public class Circle implements Shape {
    private final String id;
    private final double radius;

    public Circle(String id, double radius) {
        this.id = id;
        this.radius = radius;
    }

    @Override
    public double getArea() {
        return Math.round(3.14 * radius * radius);
    }

    @Override
    public double getPerimeter() {
        return Math.round(6.28 * radius);
    }

    @Override
    public String toString() {
        return id + "=>" + getArea() + ":" + getPerimeter();
    }
}
