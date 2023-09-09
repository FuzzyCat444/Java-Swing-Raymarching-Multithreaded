package com.fuzzycat.voxelraymarching.voxel;

import com.fuzzycat.voxelraymarching.graphics.Vector3;

public class NormalFieldGenerator {
	
	public static double[] createNormalFieldFromSignedDistanceField(double[] sdf, int width, int height, int delta) {
		int depth = sdf.length / (width * height);
		int deltaX = 1 * delta;
		int deltaY = width * delta;
		int deltaZ = width * height * delta;
		double[] normalField = new double[3 * sdf.length];
		
		int index = 0;
		int sdfIndex = 0;
		Vector3 normal1 = new Vector3();
		Vector3 normal2 = new Vector3();
		Vector3 normal3 = new Vector3();
		for (int z = 0; z < depth; z++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					
					if (x - delta < 0 || y - delta < 0 || z - delta < 0 ||
						x + delta >= width || y + delta >= height || z + delta >= depth) {
						normalField[index] = 0.0;
						normalField[index + 1] = 0.0;
						normalField[index + 2] = 0.0;
					} else {
						double horizontal1 = sdf[sdfIndex + deltaX] - sdf[sdfIndex - deltaX];
						double horizontal2 = sdf[sdfIndex + deltaY] - sdf[sdfIndex - deltaY];
						double horizontal3 = sdf[sdfIndex + deltaZ] - sdf[sdfIndex - deltaZ];
						normal1.set(horizontal1, horizontal2, horizontal3);
						
						double diagonal1 = sdf[sdfIndex + deltaX + deltaY + deltaZ] - sdf[sdfIndex - deltaX - deltaY - deltaZ];
						double diagonal2 = sdf[sdfIndex - deltaX + deltaY + deltaZ] - sdf[sdfIndex + deltaX - deltaY - deltaZ];
						double diagonal3 = sdf[sdfIndex + deltaX - deltaY + deltaZ] - sdf[sdfIndex - deltaX + deltaY - deltaZ];
						double diagonal4 = sdf[sdfIndex + deltaX + deltaY - deltaZ] - sdf[sdfIndex - deltaX - deltaY + deltaZ];
						normal2.set(0.0, 0.0, 0.0);
						normal3.set(diagonal1, diagonal1, diagonal1);
						normal2.add(normal3);
						normal3.set(-diagonal2, diagonal2, diagonal2);
						normal2.add(normal3);
						normal3.set(diagonal3, -diagonal3, diagonal3);
						normal2.add(normal3);
						normal3.set(diagonal4, diagonal4, -diagonal4);
						normal2.add(normal3);
						
						normal1.scale(0.5);
						normal1.add(normal2);
						normal1.normalize();
						
						normalField[index] = normal1.x;
						normalField[index + 1] = normal1.y;
						normalField[index + 2] = normal1.z;
					}
					
					index += 3;
					sdfIndex++;
				}
			}
		}
		
		return normalField;
	}
}
