package com.fuzzycat.voxelraymarching.voxel;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class VoxelFile {
	
	public static double[] createDistanceMapFromBitmap(int[] bitmap, int width, int height, int depth) {
		double[] map = new double[width * height * depth];
		
		int intIndex = 0;
		int bitIndex = 0;
		int distanceMapIndex = 0;
		while (distanceMapIndex < map.length) {
			int voxel = (bitmap[intIndex] >> bitIndex) & 1;
			if (voxel == 0) {
				map[distanceMapIndex] = Double.POSITIVE_INFINITY;
			} else {
				map[distanceMapIndex] = 0.0;
			}
			bitIndex++;
			if (bitIndex == 32) {
				bitIndex = 0;
				intIndex++;
			}
			distanceMapIndex++;
		}
		
		return map;
	}
	
	/* Takes a voxel data text file in the format from: https://drububu.com/miscellaneous/voxelizer/?out=txt
	 * and converts it to a bitmap representation where each voxel is either a 1 or 0. */
	public static int[] loadDrububuTextAsBitmap(String filename, int width, int height, int depth) {
		int[] map = new int[(width * height * depth + 31) / 32];
		for (int i = 0; i < map.length; i++) {
			map[i] = 0;
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(filename)))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	String[] coordinateStr = line.split(", ");
		    	if (coordinateStr.length < 3)
		    		continue;
		    	int x = 0, y = 0, z = 0;
		    	try {
			    	x = Integer.parseInt(coordinateStr[0]);
			    	y = Integer.parseInt(coordinateStr[1]);
			    	z = Integer.parseInt(coordinateStr[2]);
		    	} catch (NumberFormatException e) {
		    		continue;
		    	}
		    	if (x < 0 || y < 0 || z < 0 ||
		    		x >= width || y >= height || z >= depth)
		    		continue;
		    	setVoxelAt(map, width, height, depth, x, y, z, 1);
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return map;
	}
	
	/* Writes bitmap of voxels to disk in compressed format. Repetitions of 1's and 0's are counted and 
	 * the count is saved as a single integer. */
	public static void saveBitmap(String outputFilename, int[] map) {
		try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(outputFilename)))) {
			int intIndex = 0;
			int bitIndex = 0;
			int value = 0;
			while (true) {
				int num = 0;
				boolean done = false;
				while (((map[intIndex] >> bitIndex) & 1) == value) {
					num++;
					bitIndex++;
					if (bitIndex == 32) {
						bitIndex = 0;
						intIndex++;
						if (intIndex == map.length) {
							done = true;
							break;
						}
					}
				}
				dos.writeInt(num);
				if (done)
					break;
				value = value == 0 ? 1 : 0;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Loads compressed voxel bitmap. See saveBitmap(). */
	public static int[] loadBitmap(String inputFilename, int width, int height, int depth) {
		int[] map = new int[(width * height * depth + 31) / 32];
		try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(inputFilename)))) {
			int intIndex = 0;
			int bitIndex = 0;
			int value = 0;
			while (true) {
				int num = 0;
				try {
					num = dis.readInt();
				} catch (EOFException e) {
					break;
				}
				for (int i = 0; i < num; i++) {
					map[intIndex] = map[intIndex] | (value << bitIndex);
					bitIndex++;
					if (bitIndex == 32) {
						bitIndex = 0;
						intIndex++;
					}
				}
				value = value == 0 ? 1 : 0;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
	
	/* Takes a rectangular voxel bitmap and pads it with 0's to be a cube shaped grid. If cubeWidth is smaller than the
	 * longest side of the rectangular bitmap, then it is set to the longest side. */
	public static int[] padToCubeBitmap(int[] map, int width, int height, int depth, int cubeWidth) {
		int max = width;
		max = max > height ? max : height;
		max = max > depth ? max : depth;
		if (cubeWidth < max)
			cubeWidth = max;
		
		int[] newMap = new int[(cubeWidth * cubeWidth * cubeWidth + 31) / 32];
		for (int i = 0; i < newMap.length; i++) {
			newMap[i] = 0;
		}
		
		int zStart = (cubeWidth - depth) / 2;
		int yStart = (cubeWidth - height) / 2;
		int xStart = (cubeWidth - width) / 2;
		for (int z = 0; z < depth; z++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int voxel = getVoxelAt(map, width, height, depth, x, y, z);
					setVoxelAt(newMap, cubeWidth, cubeWidth, cubeWidth, 
							x + xStart, y + yStart, z + zStart, voxel);
				}
			}
		}
		
		return newMap;
	}
	
	private static final int NONE = 0, HOLLOW = 1, SOLID = 2;
	
	/* Fills all closed regions of voxel bitmap that are hollow, turning voxel shells into voxel solids. This helps
	 * for generating signed distance fields as opposed to regular distance fields. */
	public static void fillHollowsBitmap(int[] map, int width, int height, int depth) {
		int[] map2 = new int[(width * height * depth * 2 + 31) / 32];
		for (int i = 0; i < map2.length; i++) {
			map2[i] = 0;
		}

		VoxelCoordinate v = new VoxelCoordinate(0, 0, 0);
		for (int z = 0; z < depth; z++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (getVoxelAt(map, width, height, depth, x, y, z) == 1) {
						continue;
					}
					
					boolean hasHollowNeighbor = false;
					boolean hasSolidNeighbor = false;
					v.set(x, y, z);
					
					for (VoxelCoordinate n : v) {
						if (n.isValid(width, height, depth)) {
							int voxel = getVoxelAt2(map2, width, height, depth, n.x, n.y, n.z);
							if (voxel == HOLLOW) {
								hasHollowNeighbor = true;
							} else if (voxel == SOLID) {
								hasSolidNeighbor = true;
							}
						} else {
							hasHollowNeighbor = true;
						}
					}
					
					if (hasHollowNeighbor) {
						if (hasSolidNeighbor) {
							setVoxelAt2(map2, width, height, depth, x, y, z, SOLID);
							fillSolidToHollow(map2, width, height, depth, x, y, z);
						} else {
							setVoxelAt2(map2, width, height, depth, x, y, z, HOLLOW);
						}
					} else {
						setVoxelAt2(map2, width, height, depth, x, y, z, SOLID);
					}
				} 
			}
		}
		
		for (int z = 0; z < depth; z++) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (getVoxelAt2(map2, width, height, depth, x, y, z) == SOLID) {
						setVoxelAt(map, width, height, depth, x, y, z, 1);
					}
				}
			}
		}
	}
	
	/* Fills all adjacent SOLID voxels to HOLLOW. Helper function for fillHollowsBitmap(). */
	private static void fillSolidToHollow(int[] map2, int width, int height, int depth, int x, int y, int z) {
		HashSet<VoxelCoordinate> solidSet = new HashSet<VoxelCoordinate>();
		HashSet<VoxelCoordinate> tempSet = new HashSet<VoxelCoordinate>();
		
		solidSet.add(new VoxelCoordinate(x, y, z));

		while (!solidSet.isEmpty()) {
			for (VoxelCoordinate vc : solidSet) {
				setVoxelAt2(map2, width, height, depth, vc.x, vc.y, vc.z, HOLLOW);
			}
			tempSet.clear();
			for (VoxelCoordinate vc : solidSet) {
				for (VoxelCoordinate n : vc) {
					if (n.isValid(width, height, depth) &&
						getVoxelAt2(map2, width, height, depth, n.x, n.y, n.z) == SOLID) {
						tempSet.add(n);
					}
				}
			}
			HashSet<VoxelCoordinate> t = solidSet;
			solidSet = tempSet;
			tempSet = t;
		}
	}
	
	public static int getVoxelAt(int[] map, int width, int height, int depth, int x, int y, int z) {
		int voxelIndex = x + y * width + z * width * height;
		int intIndex = voxelIndex / 32;
		int bitIndex = voxelIndex % 32;
		return (map[intIndex] >> bitIndex) & 1;
	}
	
	public static void setVoxelAt(int[] map, int width, int height, int depth, int x, int y, int z, int value) {
		int voxelIndex = x + y * width + z * width * height;
		int intIndex = voxelIndex / 32;
		int bitIndex = voxelIndex % 32;
		map[intIndex] &= ~(1 << bitIndex);
		map[intIndex] |= (value & 1) << bitIndex;
	}
	
	public static int getVoxelAt2(int[] map2, int width, int height, int depth, int x, int y, int z) {
		int voxelIndex = x + y * width + z * width * height;
		int intIndex = voxelIndex / 16;
		int bitIndex = 2 * (voxelIndex % 16);
		return (map2[intIndex] >> bitIndex) & 3;
	}
	
	public static void setVoxelAt2(int[] map2, int width, int height, int depth, int x, int y, int z, int value) {
		int voxelIndex = x + y * width + z * width * height;
		int intIndex = voxelIndex / 16;
		int bitIndex = 2 * (voxelIndex % 16);
		map2[intIndex] &= ~(3 << bitIndex);
		map2[intIndex] |= (value & 3) << bitIndex;
	}
	
	/* Represents a position in a voxel grid. Iterating over it with a foreach loop iterates through its 6 neighbors. */
	public static class VoxelCoordinate implements Iterable<VoxelCoordinate> {
		public int x, y, z;
		public VoxelCoordinate(int x, int y, int z) {
			set(x, y, z);
		}
		
		public void set(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public boolean isValid(int width, int height, int depth) {
			return x >= 0 && x < width &&
				   y >= 0 && y < height &&
				   z >= 0 && z < depth;
		}
		
		@Override
		public int hashCode() {
			int result = x;
			result = 37 * result + y;
			result = 37 * result + z;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			VoxelCoordinate other = (VoxelCoordinate) obj;
			return other.x == this.x && other.y == this.y && other.z == this.z;
		}

		@Override
		public Iterator<VoxelCoordinate> iterator() {
			return new Iterator<VoxelFile.VoxelCoordinate>() {
				private int i = 0;
				
				@Override
				public VoxelCoordinate next() {
					VoxelCoordinate v = new VoxelCoordinate(0, 0, 0);
					switch (i) {
					case 0:
						v.set(x - 1, y, z);
						break;
					case 1:
						v.set(x + 1, y, z);
						break;
					case 2:
						v.set(x, y - 1, z);
						break;
					case 3:
						v.set(x, y + 1, z);
						break;
					case 4:
						v.set(x, y, z - 1);
						break;
					case 5:
						v.set(x, y, z + 1);
						break;
					}
					i++;
					return v;
				}
				
				@Override
				public boolean hasNext() {
					return i < 6;
				}
			};
		}
	}
}
