import org.jboss.seam.core.Interpolator;

public class TestJan {

	public static void main(String[] args) {
		Interpolator interpolator = new Interpolator();

		String interpolate = interpolator.interpolate("File upload of file {0} with content-type {1}", "filename", "contenttype");

		System.out.println(interpolate);
	}
}
