package org.frett27.spatialflink.tools;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;

/**
 * This tool class create a multi part polygon from OSM parts
 * 
 * @author use
 * 
 */
public class PolygonCreator {

	public static Logger logger = LoggerFactory.getLogger(PolygonCreator.class);

	private static String dump(List<MultiPathAndRole> l) {
		if (l == null)
			return null;

		StringBuffer sb = new StringBuffer('\n');
		for (MultiPathAndRole r : l) {
			sb.append(r).append('\n');
		}
		return sb.toString();
	}

	/**
	 * convert the two arrays in one object list
	 * 
	 * @param multiPath
	 * @param roles
	 * @return
	 */
	private static List<MultiPathAndRole> create(MultiPath[] multiPath,
			Role[] roles) {

		assert multiPath != null;
		assert roles != null;
		assert multiPath.length == roles.length;

		List<MultiPathAndRole> pathLeft = new ArrayList<>();
		for (int i = 0; i < multiPath.length; i++) {
			pathLeft.add(new MultiPathAndRole(multiPath[i], roles[i]));
		}
		return pathLeft;
	}

	/**
	 * create polygon
	 * 
	 * @param multiPath
	 * @param roles
	 * @return
	 * @throws Exception
	 */
	public static Polygon createPolygon(MultiPath[] multiPath, Role[] roles)
			throws Exception {
		return createPolygon(multiPath, roles, null);
	}

	/**
	 * create polygon, and use report object to report errors
	 * 
	 * @param multiPath
	 * @param roles
	 * @param reporter
	 * @return
	 * @throws Exception
	 */
	public static Polygon createPolygon(MultiPath[] multiPath, Role[] roles,
			IInvalidPolygonConstructionFeedBack reporter) throws Exception {
		return createPolygon(create(multiPath, roles), reporter);
	}

	/**
	 * create a polygon from multi path elements, passed arrays must have the
	 * same number of elements
	 * 
	 * @param multiPath
	 * @param roles
	 * @return
	 * @throws Exception
	 */
	public static Polygon createPolygon(final List<MultiPathAndRole> pathLeft,
			IInvalidPolygonConstructionFeedBack optionalReport)
			throws Exception {

		logger.debug("start create Polygon");

		Polygon finalPolygon = new Polygon();

		if (logger.isDebugEnabled()) {
			logger.debug("-- initial current stack :");
			logger.debug(dump(pathLeft));
			logger.debug("--end");

		}
		assert pathLeft != null;
		List<MultiPathAndRole> originalList = new ArrayList<>(pathLeft); // for
																			// dumping

		MultiPathAndRole current = null;

		current = pop(pathLeft);

		if (logger.isDebugEnabled())
			logger.debug("get the first element " + current);

		if (logger.isDebugEnabled()) {
			logger.debug("--current stack :");
			logger.debug(dump(pathLeft));
			logger.debug("--end");

		}

		while (current != null) {

			if (logger.isDebugEnabled())
				logger.debug("current :" + current);

			// autoclosed paths
			while (current != null && isClosed(current.getMultiPath())) {
				// add to polygon
				logger.debug("current is closed, add to finalPolygon");
				finalPolygon.add(current.getMultiPath(), false);

				if (logger.isDebugEnabled()) {
					logger.debug(" -- stack");
					logger.debug(dump(pathLeft));
				}

				current = pop(pathLeft);

				if (logger.isDebugEnabled()) {
					logger.debug("--current stack :");
					logger.debug(dump(pathLeft));
					logger.debug("--end");

				}
			}

			// current might be null (no way left)

			if (current == null) {
				logger.debug("current is null, end of the construction");
				return finalPolygon;
			}

			assert current != null && !current.getMultiPath().isClosedPath(0);

			MultiPath p = (MultiPath) current.getMultiPath().copy();

			boolean finished = false;

			logger.debug("having an initial element");

			while (!finished) {

				if (logger.isDebugEnabled())
					logger.debug("current :"
							+ firstAndLastPoints(null, p).toString());

				int pathEnd = p.getPathEnd(0) - 1;
				Point joinPoint = p.getPoint(pathEnd); // the join point

				if (logger.isDebugEnabled())
					logger.debug("search for lines in stack having "
							+ joinPoint);

				MultiPath followingPathWithCorrectOrder = findExtremisAndIfEndPointReverseTheMultiPath(
						pathLeft, joinPoint, current.role); // search for
																	// the
																	// next
				if (logger.isDebugEnabled()) {
					logger.debug("found multipath :"
							+ followingPathWithCorrectOrder);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("--current stack :");
					logger.debug(dump(pathLeft));
					logger.debug("--end");
				}

				if (followingPathWithCorrectOrder != null) {

					logger.debug("OK, insert the element in the current constructed polygon");

					// add the path to the current multipath

					p.insertPoints(0, -1, followingPathWithCorrectOrder, 0, 1,
							followingPathWithCorrectOrder.getPointCount() - 1,
							true); // skip the first point

				} else {
					// don't find a following path, and not closed !!!

					// Construct a JSON with all elements, for debugging or
					// correct the initial geometry

					StringBuffer sb = new StringBuffer();

					sb.append("{");
					sb.append("   \"origin\": ");
					sb.append("[");
					boolean first = true;
					for (int i = 0; i < originalList.size(); i++) {

						if (!first)
							sb.append(",");

						sb.append("{ \"geometry\" :");
						sb.append(
								GeometryEngine.geometryToJson(4623,
										originalList.get(i).multiPath)).append(
								",");
						sb.append(" \"role\": ").append('"')
								.append(originalList.get(i).role).append('"')
								.append("}");
						first = false;

					}
					sb.append("]");
					sb.append(",");
					sb.append("   \"constructed\":");
					sb.append(GeometryEngine.geometryToJson(4623, p));
					sb.append(",");
					sb.append("   \"left\":");
					sb.append("[");
					first = true;
					for (int i = 0; i < pathLeft.size(); i++) {
						MultiPathAndRole e = pathLeft.get(i);

						if (!first)
							sb.append(",");
						sb.append("{ \"geometry\" :");
						sb.append(
								GeometryEngine.geometryToJson(4623,
										e.getMultiPath())).append(",");
						sb.append(" \"role\": \"").append(e.getRole())
								.append("\"}");
						first = false;
					}
					sb.append("]");
					sb.append("}");

//					System.out.println("fail to construct poly :"
//							+ sb.toString());

					if (optionalReport != null) {
						try {
							// call the reporting object
							optionalReport.polygonCreationFeedBackReport(
									originalList, sb.toString());
						} catch (Throwable t) {
							// silent exception
							logger.error(
									"error in reporting : " + t.getMessage(), t);
						}
					}
					// raise exception for polygon construct
					throw new Exception("path cannot be closed, pathLeft :" + pathLeft.size() + " originalsize :" + originalList.size());
				}

				// closed ???

				logger.debug("is closed ?");
				if (areCoincident(p.getPoint(p.getPathStart(0)),
						p.getPoint(p.getPathEnd(0) - 1))) {

					logger.debug("yes, fire the new path");
					// yes this is closed, add the part

					// FIXME reverse path ??? -> inner / outer, the proper
					// orientation

					finalPolygon.add(p, false);
					finished = true;
				} else {
					logger.debug("no, the path is not closed, continue");
				}

			} // !finished

			logger.debug("next ring");

			if (logger.isDebugEnabled()) {
				logger.debug("--elements left to handle - current stack :");
				logger.debug(dump(pathLeft));
				logger.debug("--end");

			}

			// next path
			current = pop(pathLeft);

		}

		logger.debug("end of construct");

		return finalPolygon;

	}

	/**
	 * create a polygon from multi path elements, passed arrays must have the
	 * same number of elements
	 * 
	 * @param multiPath
	 * @param roles
	 * @return
	 * @throws Exception
	 */
	public static Polygon createPolygon(final List<MultiPathAndRole> pathLeft)
			throws Exception {
		// call the method with no report
		return createPolygon(pathLeft, null);
	}

	public static boolean isClosed(MultiPath p) {
		assert p != null;
		int start = p.getPathStart(0);
		int end = p.getPathEnd(0) - 1;

		return areCoincident(p.getPoint(start), p.getPoint(end));

	}

	public static List<Integer> findAll(List<MultiPathAndRole> left,
			Point joinPoint, Role searchRole) {
		ArrayList<Integer> indices = new ArrayList<Integer>();

		for (int i = 0; i < left.size(); i++) {

			MultiPathAndRole e = left.get(i);

			if (e.getRole() != searchRole && e.getRole() != Role.UNDEFINED)
				continue;

			MultiPath p = e.getMultiPath();
			assert p != null;
			assert p.getPathCount() == 1;

			int indexStart = p.getPathStart(0);
			int indexStop = p.getPathEnd(0) - 1;

			Point startPoint = p.getPoint(indexStart);
			Point entPoint = p.getPoint(indexStop);
			if (areCoincident(startPoint, joinPoint)
					|| areCoincident(entPoint, joinPoint)) {
				indices.add(i);
			}

		}

		return indices;
	}

	public static MultiPath findExtremisAndIfEndPointReverseTheMultiPath(
			List<MultiPathAndRole> left, Point joinPoint, Role searchRole) {
		assert left != null;

		if (left.size() == 0) {
			return null;
		}

		int bestindex = -1;
		double distance = Double.MAX_VALUE;
		MultiPath bestBet = null;

		for (int i = 0; i < left.size(); i++) {

			MultiPathAndRole e = left.get(i);

//			if (e.getRole() != searchRole && e.getRole() != Role.UNDEFINED)
//				continue;

			MultiPath p = e.getMultiPath();
			assert p != null;
			assert p.getPathCount() == 1;

			int indexStart = p.getPathStart(0);
			int indexStop = p.getPathEnd(0) - 1;

			Point startPoint = p.getPoint(indexStart);
			Point entPoint = p.getPoint(indexStop);
			if (areCoincident(startPoint, joinPoint)) {
				
				// remove point in the collection
				left.remove(i);
				return p;

			} else if (areCoincident(entPoint, joinPoint)) {

				// reverse the order
				MultiPath newGeometry = (MultiPath) p.copy();
				newGeometry.reverseAllPaths();

				left.remove(i);
				return newGeometry;
			}

			// metrics
			double d = Math.min(euclidianDistance(startPoint, joinPoint),
					euclidianDistance(entPoint, joinPoint));
			if (d < distance) {
				distance = d;
				bestindex = i;
				bestBet = p;
			}
			//

		}

//		 System.out.println("best probable match for point "
//		 + joinPoint
//		 + " index "
//		 + bestindex
//		 + "("
//		 + GeometryEngine.geometryToJson(4623, left.get(bestindex)
//		 .getMultiPath()) + ") distance :" + distance);
//		 
//		 if (bestindex != -1)
//		 {
//			 int indexStart = bestBet.getPathStart(0);
//				int indexStop = bestBet.getPathEnd(0) - 1;
//
//			 Point startPoint = bestBet.getPoint(indexStart);
//				Point entPoint = bestBet.getPoint(indexStop);
//				if (areCoincident(startPoint, joinPoint)) {
//					
//					// remove point in the collection
//					left.remove(bestindex);
//					return bestBet;
//
//				} else if (areCoincident(entPoint, joinPoint)) {
//
//					// reverse the order
//					MultiPath newGeometry = (MultiPath) bestBet.copy();
//					newGeometry.reverseAllPaths();
//
//					left.remove(bestindex);
//					return newGeometry;
//				}
//
//		 }
//		 
		 

		return null;
	}

	public static boolean areCoincident(Point p1, Point p2) {
		assert p1 != null;
		assert p2 != null;

		double euclidianDistance = euclidianDistance(p1, p2);
		 return euclidianDistance < 1e-14;
		//return euclidianDistance < 1e-5;

	}

	private static double euclidianDistance(Point p1, Point p2) {
		assert p1 != null;
		assert p2 != null;
		return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2)
				+ Math.pow(p1.getY() - p2.getY(), 2));
	}

	/**
	 * pop the first multipath in the collection, null if none
	 */
	public static MultiPathAndRole pop(List<MultiPathAndRole> l) {
		if (l == null)
			return null;

		boolean finished = false;
		MultiPathAndRole e = null;
		while (!finished) {
			if (l.size() == 0)
				return null;

			e = l.get(0);
			l.remove(0);

			MultiPath m = e.getMultiPath();
			if (m != null && !m.isEmpty()) {
				finished = true;
			}
		}

		return e;
	}

	public static void dump(MultiPath p) {
		assert p != null;
		String jsong = GeometryEngine.geometryToJson(4623, p);
		System.out.println(jsong);
	}

	static StringBuffer firstAndLastPoints(String role, MultiPath multiPath) {
		StringBuffer sb = new StringBuffer();
		if (role != null) {
			sb.append("(")

			.append(role).append(")");
		}

		sb.append(" ")
				.append(multiPath.getPointCount())
				.append(" pts -> ")
				.append(GeometryEngine.geometryToJson(4623,
						multiPath.getPoint(multiPath.getPathStart(0))));
		sb.append(" - ").append(
				GeometryEngine.geometryToJson(4623,
						multiPath.getPoint(multiPath.getPathEnd(0) - 1)));
		return sb;
	}

}
