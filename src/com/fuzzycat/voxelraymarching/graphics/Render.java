package com.fuzzycat.voxelraymarching.graphics;

public class Render {
	private Camera camera;
	private RenderThread[] threads;
	private boolean begun;
	
	// Change number of threads to suit your CPU capabilities
	public Render(int width, int height, double fov, int threadCount) {
		camera = new Camera();
		camera.width = width;
		camera.height = height;
		camera.fov = fov;
		camera.position = new Vector3();
		camera.yaw = 0.0;
		camera.pitch = 0.0;
		camera.rays = new Vector3[width * height];
		threads = new RenderThread[threadCount];
		
		double halfFov = 0.5 * fov;
		double halfWidth = 0.5 * width;
		double halfHeight = 0.5 * height;
		double rz = -halfWidth / Math.tan(Math.toRadians(halfFov));
		
		double ry = -halfHeight + 0.5;
		for (int y = 0; y < height; y++) {
			double rx = -halfWidth + 0.5;
			for (int x = 0; x < width; x++) {
				Vector3 pixelRay = new Vector3(rx, ry, rz);
				pixelRay.normalize();
				camera.rays[x + y * width] = pixelRay;
				
				rx += 1.0;
			}
			ry += 1.0;
		}
	}
	
	/* Start all render threads, they will all wait for their job every frame. */
	public void begin() {
		begun = true;
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new RenderThread();
			threads[i].start();
		}
	}
	
	/* Close all render threads. */
	public void end() {
		begun = false;
		for (int i = 0; i < threads.length; i++) {
			threads[i].stopRunning();
		}
		for (int i = 0; i < threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			threads[i] = null;
		}
	}
	
	public void rayMarchVoxels(RenderContext context) {
		if (!begun)
			return;
		
		// Begin rendering on every thread
		for (int i = 0; i < threads.length; i++) {
			threads[i].startRender(context, camera, i, threads.length);
		}
		// Wait for every thread to finish rendering
		for (int i = 0; i < threads.length; i++) {
			synchronized (threads[i]) {
				while (threads[i].isRendering()) {
					try {
						threads[i].wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public Camera getCamera() {
		return camera;
	}
}
