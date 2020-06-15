package testy;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import java.awt.image.DataBufferByte;

public class Projekt_v7fit {
	static BufferedImage Mat2BufferedImage(Mat m) {
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;
	}

	public static void main(String args[]) throws InterruptedException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat obrazSzary = new Mat();
		Mat obrazBinarny = new Mat();
		Mat hierarchy = new Mat();
		Mat obraz = new Mat();
		VideoCapture cap = new VideoCapture();
		Thread.sleep(500); 
		JFrame jframe = new JFrame("Video Title");
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel vidPanel = new JLabel();
		jframe.setContentPane(vidPanel);
		jframe.setSize(600, 600);
		jframe.setVisible(true);
		cap.open(0);
		if (!cap.isOpened()) {
			System.out.println("Did not connect to camera");
		} else
			System.out.println("found webcam: " + cap.toString());
		while (true) {
			if (cap.read(obraz)) {
				Thread.sleep(200);
				ArrayList<MatOfPoint> listaKonturow = new ArrayList<MatOfPoint>();
				Imgproc.cvtColor(obraz, obrazSzary, Imgproc.COLOR_BGR2GRAY);
				Imgproc.blur(obrazSzary, obrazSzary, new Size(15, 15));
				Imgproc.threshold(obrazSzary, obrazBinarny, 140, 255, Imgproc.THRESH_TOZERO_INV);
				Imgproc.findContours(obrazBinarny, listaKonturow, hierarchy, Imgproc.RETR_LIST,
				Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
				Mat konturMat = new Mat();
				konturMat.create(obrazSzary.rows(), obrazSzary.cols(), CvType.CV_8UC3);
				int largestContourIndex = 0;
				MatOfPoint largestContour = null;
				double largestArea = 0;
				int contourNumber = listaKonturow.size();
				double contourArea = 0;
				for (int i = 0; i < contourNumber; i++) {
					 contourArea = Imgproc.contourArea(listaKonturow.get(i));
					if (contourArea > largestArea) {
						largestArea = contourArea;
						largestContourIndex = i;
					}

				}
				largestContour = listaKonturow.get(largestContourIndex);
				MatOfInt convexHull = new MatOfInt();
				Imgproc.convexHull(largestContour, convexHull);
				List<Point[]> hullpoints = new ArrayList<Point[]>();
				Point[] points = new Point[convexHull.rows()];
				for (int j = 0; j < convexHull.rows(); j++) {
					int index = (int) convexHull.get(j, 0)[0];
					points[j] = new Point(largestContour.get(index, 0)[0], largestContour.get(index, 0)[1]);
				}
				hullpoints.add(points);
				List<MatOfPoint> hulls = new ArrayList<MatOfPoint>();
				MatOfPoint mop = new MatOfPoint();
				mop.fromArray(hullpoints.get(0));
				hulls.add(mop);
				Imgproc.drawContours(konturMat, listaKonturow, largestContourIndex, new Scalar(100, 100, 100), 1, 8, new Mat(), 0, new Point());
				Imgproc.drawContours(konturMat, hulls, 0, new Scalar(35, 200, 212), 1, 8, new Mat(), 0, new Point());
				MatOfInt4 defects = new MatOfInt4();
				Imgproc.convexityDefects(largestContour, convexHull, defects);
				int max_points =111;
				int numPoints = 0;
			    numPoints = (int) defects.total();
				if (numPoints > max_points) {
					System.out.println("Processing " + max_points + " defect pts");
					numPoints = max_points;
				}
				System.out.println(numPoints);
				Point[] tipPts = new Point[numPoints];
				Point[] endPts = new Point[numPoints];
				Point[] foldPts = new Point[numPoints];
				for (int i = 0; i < numPoints; i++) {
					double[] dat = defects.get(i, 0);
					double[] startdat = largestContour.get((int) dat[0], 0);
					Point startPt = new Point(startdat[0], startdat[1]);
					tipPts[i] = startPt;
					double[] enddat = largestContour.get((int) dat[1], 0);
					endPts[i] = new Point(enddat[0], enddat[1]);
					double[] depthdat = largestContour.get((int) dat[2], 0);
					Point depthPt = new Point(depthdat[0], depthdat[1]);
					foldPts[i] = depthPt;
					double[] depths = new double[numPoints];
					depths[i] = dat[3];
				}
				for (int i = 0; i < numPoints; i++) {
					Imgproc.circle(konturMat, tipPts[i], 5, new Scalar(0, 0, 255));
					Imgproc.circle(konturMat, foldPts[i], 5, new Scalar(255, 0, 0));
					Imgproc.circle(konturMat, endPts[i], 5, new Scalar(0, 255, 0));
					Imgproc.line(konturMat, tipPts[i], foldPts[i], new Scalar(0, 0, 255));
					// Imgproc.line(konturMat, tipPts[i], endPts[i], new Scalar(255, 0, 255));
					Imgproc.line(konturMat, endPts[i], foldPts[i], new Scalar(0, 0, 255));
				}

				ImageIcon image = new ImageIcon(Mat2BufferedImage(konturMat));
				vidPanel.setIcon(image);
				vidPanel.repaint();

			}
		}

	}
}
