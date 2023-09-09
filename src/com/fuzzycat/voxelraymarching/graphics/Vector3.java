package com.fuzzycat.voxelraymarching.graphics;

public class Vector3 {
	
	public double x, y, z;
	
	public Vector3() {
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
	}
	
	public Vector3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3(Vector3 other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}
	
	public void set(Vector3 other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}
	
	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double dot(Vector3 other) {
		return x * other.x + y * other.y + z * other.z;
	}
	
	public void cross(Vector3 other, Vector3 result) {
		result.x = y * other.z - z * other.y;
		result.y = z * other.x - x * other.z;
		result.z = x * other.y - z * other.x;
	}
	
	public void add(Vector3 other) {
		this.x += other.x;
		this.y += other.y;
		this.z += other.z;
	}
	
	public void sub(Vector3 other) {
		this.x -= other.x;
		this.y -= other.y;
		this.z -= other.z;
	}
	
	public void scale(double s) {
		x *= s;
		y *= s;
		z *= s;
	}
	
	public double lenSq() {
		return x * x + y * y + z * z;
	}
	
	public double len() {
		return Math.sqrt(x * x + y * y + z * z);
	}
	
	public void normalize() {
		double magnitudeInverse = 1.0 / Math.sqrt(x * x + y * y + z * z);
		if (magnitudeInverse == Double.POSITIVE_INFINITY)
			return;
		x *= magnitudeInverse;
		y *= magnitudeInverse;
		z *= magnitudeInverse;
	}
	
	public void rotateZX(double cos, double sin) {
		double zNew = z * cos - x * sin;
		double xNew = x * cos + z * sin;
		z = zNew;
		x = xNew;
	}
	
	public void rotateZX(double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double zNew = z * cos - x * sin;
		double xNew = x * cos + z * sin;
		z = zNew;
		x = xNew;
	}
	
	public void rotateYZ(double cos, double sin) {
		double yNew = y * cos - z * sin;
		double zNew = z * cos + y * sin;
		y = yNew;
		z = zNew;
	}
	
	public void rotateYZ(double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double yNew = y * cos - z * sin;
		double zNew = z * cos + y * sin;
		y = yNew;
		z = zNew;
	}
	
	public void rotateXY(double cos, double sin) {
		double xNew = x * cos - y * sin;
		double yNew = y * cos + x * sin;
		x = xNew;
		y = yNew;
	}
	
	public void rotateXY(double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double xNew = x * cos - y * sin;
		double yNew = y * cos + x * sin;
		x = xNew;
		y = yNew;
	}
}
