# Java-Swing-Raymarching-Multithreaded

Video: https://www.youtube.com/watch?v=0KcN5l9NG0Q

This is a simple Java Swing project where we take a voxel bitmap (array of bits), convert it to a distance field (array of double) using the marching parabolas algorithm, generate a normal field from the distance field (array of doubles, 3 doubles per normal). The distance field tells us how far away the closest voxel is, allowing rays from the camera to be marched through the voxel grid in an optimal manner. Once a hit is found, diffuse lighting calculations are made and reflections are done via the normal field, and a 360 degree photosphere is sampled for environmental reflections. Example models are included, with instructions in the Main.java file to achieve the renders shown in the video. This application is realtime and multithreaded and is set to 12 threads by default, change this in the Main.java file.
