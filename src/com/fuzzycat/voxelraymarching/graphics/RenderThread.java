package com.fuzzycat.voxelraymarching.graphics;

import com.fuzzycat.distancefunction.util.MathUtil;

public class RenderThread extends Thread {
	private boolean running;
	private boolean render;
	
	private RenderContext ctx;
	private Camera cam;
	private int index;
	private int indexStride;
	
	public RenderThread() {
		running = true;
		render = false;
	}
	
	@Override
	public void run() {
		
		while (true) {
			// Wait for job from Render class
			synchronized (this) {
				while (running && !render) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			if (!running) {
				break;
			} else if (render) {
				scanRows();
				
				render = false;
				synchronized (this) {
					notify();
				}
			}
		}
	}
	
	private void scanRows() {
		Vector3 ray = new Vector3();
		Vector3 march = new Vector3();
		Vector3 normal = new Vector3();
		Vector3 reflected = new Vector3();
		Vector3 tempVec = new Vector3();
		Vector3 lightDir = new Vector3(1.0, -1.0, -1.0);
		lightDir.normalize();
		double[] rayTraceResults = new double[8];
		
		PhotoSphereTrace photoSphereTrace = 
				new PhotoSphereTrace(ctx.photoSphereColor, 2 * ctx.photoSphereHeight, 
									 ctx.photoSphereHeight, 2 * ctx.photoSphereHeight / MathUtil._2_PI, 
									 ctx.photoSphereHeight / Math.PI);
		
		final double cosYaw = Math.cos(cam.yaw);
		final double sinYaw = Math.sin(cam.yaw);
		final double cosPitch = Math.cos(cam.pitch);
		final double sinPitch = Math.sin(cam.pitch);
		
		int rayIndex = index * cam.width;
		final int rayRowIncr = cam.width * (indexStride - 1);
		int pixelIndex = (cam.height - 1 - index) * cam.width;
		final int pixelRowIncr = -cam.width * (indexStride + 1);
		final int dim = ctx.voxelsDimension;
		final int dimSq = dim * dim;

		final int cubeSize = ctx.voxelsDimension - 1;
		
		while (rayIndex < cam.rays.length) {
			for (int x = 0; x < cam.width; x++) {
				ray.set(cam.rays[rayIndex]);
				
				ray.rotateYZ(cosPitch, sinPitch);
				ray.rotateZX(cosYaw, sinYaw);
				
				int resultIndex = 0;
				MathUtil.rayTraceCube(cam.position, ray, rayTraceResults);
				if (rayTraceResults[4] < rayTraceResults[0]) {
					resultIndex = 4;
				}
				if (rayTraceResults[resultIndex] != Double.POSITIVE_INFINITY) {
					march.x = rayTraceResults[resultIndex + 1] * cubeSize + 0.5;
					march.y = rayTraceResults[resultIndex + 2] * cubeSize + 0.5;
					march.z = rayTraceResults[resultIndex + 3] * cubeSize + 0.5;
					while (true) {
						int marchX = (int) march.x;
						int marchY = (int) march.y;
						int marchZ = (int) march.z;
						if (marchX < 0 || marchX >= ctx.voxelsDimension ||
						    marchY < 0 || marchY >= ctx.voxelsDimension ||
						    marchZ < 0 || marchZ >= ctx.voxelsDimension) {
							ctx.screenPixels[pixelIndex] = 0;
							break;
						}
						
						int sdfIndex = marchX + marchY * dim + marchZ * dimSq;
						int normalIndex = 3 * sdfIndex;
						double distance = ctx.sdf[sdfIndex];
						normal.set(ctx.normals[normalIndex], ctx.normals[normalIndex + 1], ctx.normals[normalIndex + 2]);
						double diffuse = Math.max(-lightDir.dot(normal), 0.3);
						if (distance < 1.0) {
							// Calculate reflected ray
							reflected.set(ray);
							tempVec.set(normal);
							tempVec.scale(2.0 * ray.dot(normal));
							reflected.sub(tempVec);

							int photoSphereColor = photoSphereTrace.color(reflected);
							int diffuseColorR = (int) (diffuse * ((ctx.diffuseColor >> 16) & 0xff));
							int diffuseColorG = (int) (diffuse * ((ctx.diffuseColor >> 8) & 0xff));
							int diffuseColorB = (int) (diffuse * (ctx.diffuseColor & 0xff));
							int photoSphereColorR = (photoSphereColor >> 16) & 0xff;
							int photoSphereColorG = (photoSphereColor >> 8) & 0xff;
							int photoSphereColorB = photoSphereColor & 0xff;
							double dsr1 = ctx.diffuseSpecularRatio;
							double dsr2 = 1.0 - ctx.diffuseSpecularRatio;
							int pixelR = (int) (diffuseColorR * dsr2 + photoSphereColorR * dsr1);
							int pixelG = (int) (diffuseColorG * dsr2 + photoSphereColorG * dsr1);
							int pixelB = (int) (diffuseColorB * dsr2 + photoSphereColorB * dsr1);
							int pixelColor = (pixelR << 16) | (pixelG << 8) | pixelB;
							
							ctx.screenPixels[pixelIndex] = pixelColor;
							break;
						}
						
						// March distance given by signed distance field
						tempVec.set(ray);
						tempVec.scale(distance);
						march.add(tempVec);
					}
				} else {
					ctx.screenPixels[pixelIndex] = 0;
				}
					
				rayIndex++;
				pixelIndex++;
			}
			
			rayIndex += rayRowIncr;
			pixelIndex += pixelRowIncr;
		}
	}
	
	public synchronized void startRender(RenderContext context, Camera cameraData, int index, int indexStride) {
		ctx = context;
		cam = cameraData;
		this.index = index;
		this.indexStride = indexStride;
		
		render = true;
		notify();
	}
	
	public boolean isRendering() {
		return render;
	}
	
	public synchronized void stopRunning() {
		running = false;
		
		notify();
	}

	private static class PhotoSphereTrace {
		private int[] psc;
		private int psw, psh;
		private double pswr, pshr;
		public PhotoSphereTrace(int[] psc, int psw, int psh, double pswr, double pshr) {
			this.psc = psc;
			this.psw = psw;
			this.psh = psh;
			this.pswr = pswr;
			this.pshr = pshr;
		}
		public int color(Vector3 ray) {
			double rayYaw = MathUtil.fastAtan2(ray.x, ray.z);
			double rayPitch = MathUtil.fastAtan2(Math.abs(ray.y), Math.sqrt(1.0 - ray.y * ray.y));
			if (ray.y < 0)
				rayPitch = -rayPitch;
			rayYaw = MathUtil._2_PI - rayYaw;
			rayPitch = MathUtil.PI_2 - rayPitch;
			
			int photoSphereX = (int) (rayYaw * pswr);
			int photoSphereY = (int) (rayPitch * pshr);
			
			if (photoSphereX < 0) photoSphereX += psw;
			else if (photoSphereX >= psw) photoSphereX -= psw;
			if (photoSphereY < 0) photoSphereY += psh;
			else if (photoSphereY >= psh) photoSphereY -= psh;
			
			return psc[photoSphereX + photoSphereY * psw];
		}
	}
}
