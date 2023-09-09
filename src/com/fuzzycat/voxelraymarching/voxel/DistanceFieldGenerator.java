package com.fuzzycat.voxelraymarching.voxel;

public class DistanceFieldGenerator {
	
	/* 'map' should be an array of two values: 0 and infinity. 0 means a solid cell and infinity means an empty cell. The returned
	 * distance field, at each cell, will contain the distance to the closest solid cell from the original map. */
	public static double[] createDistanceFieldFromMap(double[] map, int width, int height) {
		int depth = map.length / (width * height);
		double[] distanceField = new double[map.length];
		for (int i = 0; i < map.length; i++) {
			distanceField[i] = map[i];
		}
		
		int size = height > width ? height : width;
		size = depth > size ? depth : size;
		double[] envelopeVertices = new double[2 * size];
		double[] envelopeIntersections = new double[size];
		
		// XY plane pass
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				rowSquaredDistance(distanceField, envelopeVertices, envelopeIntersections, depth, x + y * width, width * height);
			}
		}
		
		// YZ plane pass
		for (int z = 0; z < depth; z++) {
			for (int y = 0; y < height; y++) {
				rowSquaredDistance(distanceField, envelopeVertices, envelopeIntersections, width, y * width + z * width * height, 1);
			}
		}
		
		// ZX plane pass
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				rowSquaredDistance(distanceField, envelopeVertices, envelopeIntersections, height, x + z * width * height, width);
			}
		}
		
		// Convert to non-squared distance field
		for (int i = 0; i < distanceField.length; i++) {
			distanceField[i] = Math.sqrt(distanceField[i]);
		}
		
		return distanceField;
	}
	
	public static double[] createSignedDistanceFieldFromMap(double[] map, int width, int height) {
		return subDistanceFields(createDistanceFieldFromMap(map, width, height), createDistanceFieldFromMap(invertMap(map), width, height));
	}

	private static double[] invertMap(double[] map) {
		double[] inverted = new double[map.length];
		for (int i = 0; i < map.length; i++) {
			if (map[i] == 0.0) {
				inverted[i] = Double.POSITIVE_INFINITY;
			} else {
				inverted[i] = 0.0;
			}
		}
		return inverted;
	}
	
	private static double[] subDistanceFields(double[] distanceFieldA, double[] distanceFieldB) {
		double[] summed = new double[distanceFieldA.length];
		for (int i = 0; i < distanceFieldA.length; i++) {
			summed[i] = distanceFieldA[i] - distanceFieldB[i];
		}
		return summed;
	}
	
	// Reference for "Marching Parabolas" algorithm: https://prideout.net/blog/distance_fields/
	private static void rowSquaredDistance(double[] distanceField, double[] envelopeVertices, double[] envelopeIntersections, int rowSize, int startIndex, int incrementIndex) {
		// Find and store envelope parabolas
		int envelopeIndex = 0;
		int envelopeIndex2 = 0;
		int index = startIndex;
		int startI = 0;
		while (startI < rowSize && distanceField[index] == Double.POSITIVE_INFINITY) {
			startI++;
			index += incrementIndex;
		}
		if (startI != rowSize) {
			envelopeIndex = 1;
			envelopeIndex2 = 2;
			envelopeVertices[0] = startI;
			envelopeVertices[1] = distanceField[index];
			index += incrementIndex;
		}
		for (int i = startI + 1; i < rowSize; i++) {
			double parabolaX = envelopeVertices[envelopeIndex2 - 2];
			double parabolaY = envelopeVertices[envelopeIndex2 - 1];
			double nextParabolaX = i;
			double nextParabolaY = distanceField[index];
			if (nextParabolaY != Double.POSITIVE_INFINITY) {
				double intersectX = intersectParabolas(parabolaX, parabolaY, nextParabolaX, nextParabolaY);
				while (envelopeIndex >= 2 && intersectX < envelopeIntersections[envelopeIndex - 2]) {
					envelopeIndex--;
					envelopeIndex2 -= 2;
					
					parabolaX = envelopeVertices[envelopeIndex2 - 2];
					parabolaY = envelopeVertices[envelopeIndex2 - 1];
					intersectX = intersectParabolas(parabolaX, parabolaY, nextParabolaX, nextParabolaY);
				}
				envelopeVertices[envelopeIndex2] = nextParabolaX;
				envelopeVertices[envelopeIndex2 + 1] = nextParabolaY;
				envelopeIntersections[envelopeIndex - 1] = intersectX;
				
				envelopeIndex++;
				envelopeIndex2 += 2;
			}
			index += incrementIndex;
		}
		if (envelopeIndex >= 1) {
			envelopeIntersections[envelopeIndex - 1] = Double.POSITIVE_INFINITY;
		}
		int parabolaCount = envelopeIndex;

		// March parabolas
		envelopeIndex = 0;
		envelopeIndex2 = 0;
		double nextIntersection = envelopeIntersections[envelopeIndex];
		index = startIndex;
		for (int i = 0; i < rowSize; i++) {
			if (parabolaCount > 0) {
				while (i > nextIntersection) {
					envelopeIndex++;
					envelopeIndex2 += 2;
					nextIntersection = envelopeIntersections[envelopeIndex];
				}
				double parabolaX = envelopeVertices[envelopeIndex2];
				double parabolaY = envelopeVertices[envelopeIndex2 + 1];
				distanceField[index] = evaluateParabola(parabolaX, parabolaY, i);
			} else {
				distanceField[index] = Double.POSITIVE_INFINITY;
			}
			
			index += incrementIndex;
		}
	}
	
	// Concerns the intersection of two parabolas in the form y = (x - px)^2 + py, where (px, py) is the location of the vertex.
	private static double intersectParabolas(double p1x, double p1y, double p2x, double p2y) {
		return 0.5 * (p1x + p2x + (p1y - p2y) / (p1x - p2x));
	}
	
	// Computes the value y(x) where y = (x - px)^2 + py
	private static double evaluateParabola(double px, double py, double x) {
		double xpx = x - px;
		return xpx * xpx + py;
	}
}
