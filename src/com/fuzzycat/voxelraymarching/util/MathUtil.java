package com.fuzzycat.voxelraymarching.util;

import com.fuzzycat.voxelraymarching.graphics.Vector3;

public class MathUtil {
	
	public static double PI_4 = Math.PI / 4.0;
	public static double PI_2 = Math.PI / 2.0;
	public static double _2_PI = 2.0 * Math.PI;
	public static double _3_PI_2 = 3.0 * PI_2;
	
	/* Reference for "arcAngle" approximation: https://www-labs.iro.umontreal.ca/~mignotte/IFT2425/Documents/EfficientApproximationArctgFunction.pdf
	 * Maximum error is 0.22 degrees. This function can also be used to compute asin(y) = atan2(y, sqrt(1 - y^2)). A table of values can be more
	 * efficient but is avoided here to yield more memory bandwidth to image buffer and distance field read/write operations. */
	private static double ATAN_CONSTANT_1 = 0.273;
	private static double ATAN_CONSTANT_2 = ATAN_CONSTANT_1 + PI_4;
	public static double fastAtan2(double y, double x) {
		double ax = x < 0 ? -x : x;
		double ay = y < 0 ? -y : y;
		// Swap between computing y/x (tan) and x/y (cot) depending on inputs to prevent division by small numbers
		if (ay < ax) {
			// Triangle in unit circle with base on the X axis
			double tangent = ay / ax;
			// Quadratic approximation to arctangent for domain [0, 1]
			double arcAngle = tangent * (ATAN_CONSTANT_2 - ATAN_CONSTANT_1 * tangent);
			if (y < 0) {
				if (x < 0) {
					return Math.PI + arcAngle; // Quadrant III
				} else {
					return _2_PI - arcAngle; // Quadrant IV
				}
			} else {
				if (x < 0) {
					return Math.PI - arcAngle; // Quadrant II
				} else {
					return arcAngle; // Quadrant I
				}
			}
		} else {
			// Triangle in unit circle with base on the Y axis
			double cotangent = ax / ay;
			// Quadratic approximation to arctangent for domain [0, 1]
			double arcAngle = cotangent * (ATAN_CONSTANT_2 - ATAN_CONSTANT_1 * cotangent);
			if (y < 0) {
				if (x < 0) {
					return _3_PI_2 - arcAngle; // Quadrant III
				} else {
					return _3_PI_2 + arcAngle; // Quadrant IV
				}
			} else {
				if (x < 0) {
					return PI_2 + arcAngle; // Quadrant II
				} else {
					return PI_2 - arcAngle; // Quadrant I
				}
			}
		}
	}
	
	/* Finds the intersections of a ray and a cube of size 1 with a corner at the origin.
	 * 'result' parameter must have a length of at least 8. */
	public static void rayTraceCube(Vector3 p, Vector3 d, double[] result) {
		result[0] = Double.POSITIVE_INFINITY;
		result[4] = Double.POSITIVE_INFINITY;
		int index = 0;
		final double c = 1.0;
		
		// Plane x = 0
		double t = -p.x / d.x;
		if (t > 0.0 && t != Double.POSITIVE_INFINITY) {
			double y = p.y + t * d.y;
			double z = p.z + t * d.z;
			if (y >= 0.0 && y <= c && z >= 0.0 && z <= c) {
				result[index++] = t;
				result[index++] = 0.0;
				result[index++] = y;
				result[index++] = z;
			}
		}
		// Plane x = c
		t = (c - p.x) / d.x;
		if (t > 0.0 && t != Double.POSITIVE_INFINITY) {
			double y = p.y + t * d.y;
			double z = p.z + t * d.z;
			if (y >= 0.0 && y <= c && z >= 0.0 && z <= c) {
				result[index++] = t;
				result[index++] = c;
				result[index++] = y;
				result[index++] = z;
				if (index == 8) {
					return;
				}
			}
		}
		// Plane y = 0
		t = -p.y / d.y;
		if (t > 0.0 && t != Double.POSITIVE_INFINITY) {
			double z = p.z + t * d.z;
			double x = p.x + t * d.x;
			if (z >= 0.0 && z <= c && x >= 0.0 && x <= c) {
				result[index++] = t;
				result[index++] = x;
				result[index++] = 0.0;
				result[index++] = z;
				if (index == 8) {
					return;
				}
			}
		}
		// Plane y = c
		t = (c - p.y) / d.y;
		if (t > 0.0 && t != Double.POSITIVE_INFINITY) {
			double z = p.z + t * d.z;
			double x = p.x + t * d.x;
			if (z >= 0.0 && z <= c && x >= 0.0 && x <= c) {
				result[index++] = t;
				result[index++] = x;
				result[index++] = c;
				result[index++] = z;
				if (index == 8) {
					return;
				}
			}
		}
		// Plane z = 0
		t = -p.z / d.z;
		if (t > 0.0 && t != Double.POSITIVE_INFINITY) {
			double x = p.x + t * d.x;
			double y = p.y + t * d.y;
			if (x >= 0.0 && x <= c && y >= 0.0 && y <= c) {
				result[index++] = t;
				result[index++] = x;
				result[index++] = y;
				result[index++] = 0.0;
				if (index == 8) {
					return;
				}
			}
		}
		// Plane z = c
		t = (c - p.z) / d.z;
		if (t > 0.0 && t != Double.POSITIVE_INFINITY) {
			double x = p.x + t * d.x;
			double y = p.y + t * d.y;
			if (x >= 0.0 && x <= c && y >= 0.0 && y <= c) {
				result[index++] = t;
				result[index++] = x;
				result[index++] = y;
				result[index++] = c;
				if (index == 8) {
					return;
				}
			}
		}
	}
}
