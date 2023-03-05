package com.biliogadafr.math;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Array;
import com.biliogadafr.gdxtest.scenes.EditorScene.Point;

public class ConvexDecompositor 
{
	private class Score
	{
		public float length;
		public float total;
	}
	
	private float[] concaveShape;
	private List<float[]> resultShapes = new ArrayList<float[]>();
	private float[] result1;
	private float[] result2;
	
	public List<float[]> getResult()
	{
		return resultShapes;
	}
	
	public void setShape(float[] points)
	{
		concaveShape = points;
	}
	
	public void compute()
	{
		resultShapes.clear();
		resultShapes.add(concaveShape);
		int i = 0;
		//Avoid recursion
		while (i<resultShapes.size())
		{
			if( convexDecomposition(resultShapes.get(i)) )
			{
				resultShapes.set(i, result1);
				resultShapes.add(result2);
			}
			else
			{
				i++;
			}
		}
	}
	
	/**
	 * Fill coordinates into points which describes line that starts from index 'i' in points array.
	 * If
	 * @param pointsArr - polygon defined by points. 
	 * @param i - the number of point(not element of array) starting from 0.
	 * @param reverse -  in which direction retrieve line
	 */
	private float[] getLineFromIndex(float[] pointsArr, int i, boolean reverse)
	{
		float[] result = new float[4];
		//Start of line
		result[0] = pointsArr[i*2];
		result[1] = pointsArr[i*2+1];
		//End of line.
		if(!reverse)
		{
			//end of line is the first point in array.
			if((i+1)>=pointsArr.length/2)
			{
				result[2] = pointsArr[0];
				result[3] = pointsArr[1];
			}
			else //end of line is next point in array.
			{
				result[2] = pointsArr[(i+1)*2];
				result[3] = pointsArr[(i+1)*2+1];
			}
		}
		else
		{
			if((i-1)<0)
			{
				result[2] = pointsArr[pointsArr.length-2];
				result[3] = pointsArr[pointsArr.length-1];
			}
			else
			{
				result[2] = pointsArr[(i-1)*2];
				result[3] = pointsArr[(i-1)*2+1];
			}
		}
		return result;
	}
	
	/**Intersection test for two lines */
	public static boolean intersectSegmentsTest (float[] first, float[] second) 
	{
		if(first.length<4 || second.length<4)
		{
			return false;		
		}
		float x1 = first[0];
		float y1 = first[1];
		float x2 = first[2]; 
		float y2 = first[3];
		float x3 = second[0];
		float y3 = second[1];
		float x4 = second[2];
		float y4 = second[3];

		float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
		if (d == 0) 
			return false;

		float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d;
		float ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / d;

		if (ua < 0 || ua > 1 || ub < 0 || ub > 1) 
			return false;

		return true;
	}
	
	/**
	 * Check for intersections inside shape
	 * @param pointsArr
	 * @return true if intersects
	 */
	private boolean intersectionInShapeTest(float[] pointsArr)
	{
		for(int i = 0; i<pointsArr.length/2;i++)
		{
			float[] firstLine = getLineFromIndex(pointsArr,i, false);
			//If check for intersection 2 segments that share 1 point they will be
			//treated as intersected. For joined segments it means that they always intersects.
			//So we must avoid checking for intersections joined segments.
			int adjoiningCorrector = 1;
			if(i!=0)
			{
				adjoiningCorrector=0;
			}
			for(int j = i+2; j<pointsArr.length/2-adjoiningCorrector; j++)
			{
				float[] secondLine = getLineFromIndex(pointsArr, j, false);	
				if(intersectSegmentsTest(firstLine, secondLine))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Check intersections for line with shape
	 * @param shape
	 * @param line
	 * @param id1 - id of point to ignore
	 * @param id2
	 * @return true if intersects
	 */
	private boolean intersectionWithShapeTest(float[] shape, float[] line, int id1, int id2)
	{
		for(int i = 0; i<shape.length/2;i++)
		{
			//Avoid checking for intersections of joined segments.
			int startOfPrevSegment1 = (id1==0?shape.length/2-1:id1-1);
			int startOfPrevSegment2 = (id2==0?shape.length/2-1:id2-1);
			if( 
					   i == id1 
					|| i == startOfPrevSegment1
					|| i == id2 
					|| i == startOfPrevSegment2 
					)
			{
				continue;
			}
			float[] shapeLine = getLineFromIndex(shape, i, false);
			if(intersectSegmentsTest(line, shapeLine))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean convexDecomposition(float[] points)
	{
		//No sense to check triangle for convex shape.
		if(points.length<4*2)
		{
			return false;
		}
		//Check for intersections inside shape
		if(intersectionInShapeTest(points))
		{
			return false;//Don't want to process shape with intersections.
		}
		//1.Trying to determine in which direction points should be processed
		boolean ccwDirection = detectDirection(points);
		//reverse direction to make it CCW.
		if(!ccwDirection)
		{
			int last = points.length-1;
			for(int i = 0; i<points.length/2;i+=2)
			{
				float tempX;
				float tempY;
				tempX = points[i];
				tempY = points[i+1];
				points[i] = points[last-i-1];
				points[i+1] = points[last-i];
				points[last-i-1]=tempX;
				points[last-i]=tempY;
			}
		}
		//2. Detect concave points.
		Array<Integer> concavePointId = new Array<Integer>();
		for(int i=0 ;i<points.length/2;i++)
		{
			//If not convex (if concave)
			if(!isConvexPoint( getLineFromIndex(points, i, false), getLineFromIndex(points, i, true)))
			{
				concavePointId.add(i);		
			}
		}
		//If shape is convex
		if(concavePointId.size==0)
		{
			return false;
		}
		int sharpestID=0;
		float sharpestAngle=180;
		//3. Get concave point with minimum angle. (The sharpest)
		//This will be start point for new connection.
		for(Integer id : concavePointId)
		{
			//Get lines
			float[] firstLine = getLineFromIndex(points, id, true);
			float[] secondLine = getLineFromIndex(points, id, false);
			//Check angle between them. 
			float angle = getAngle(firstLine, secondLine);
			if(sharpestAngle>angle)
			{
				//remember id with min angle
				sharpestID = id;
				sharpestAngle = angle;
			}
		}
		Array<Integer> matchingPointsIds = new Array<Integer>();
		Array<Score> matchingPointsScore = new Array<Score>();
		//4. Pick matching connections
		//Get angle of this corner.
		float outerAngle = getAngle(getLineFromIndex(points, sharpestID, true),getLineFromIndex(points, sharpestID, false));
		//Start of connection candidate
		float[] connection = new float[4];
		float[] reverseConnection = new float[4];
		//This line looks back(CW) from start point.
		float[] baseLine = getLineFromIndex(points, sharpestID, true);
		getPointById(sharpestID, 0, points, connection);
		reverseConnection[2] = connection[0];
		reverseConnection[3] = connection[1];
		for(int i = 0; i<points.length/2; i++)
		{
			//4.0 Don't try to make connections to the adjoining points. Skip this points.
			//Also skip connection with start point itself
			//Please... no! Don't look at this!
			if(i ==  sharpestID || i == (sharpestID==0?points.length/2-1:sharpestID-1) || i == (sharpestID==points.length/2-1?0:sharpestID+1))
			{
				//Just skipping current index, previous index and next index.
				//Because shape is cyclic if index is 0 then prev index will be last index in shape
				//If index is last in the shape then next index is 0.
				continue;
			}
			getPointById(i, 2, points, connection);
			reverseConnection[0]=connection[2];
			reverseConnection[1]=connection[3];
			//4.1. Filter connections that lies inside the shape.
			float relativeConnectionAngle = getFullAngle(baseLine, connection);
			boolean matchByAngle = relativeConnectionAngle>outerAngle;
			//4.2. Filter connections without intersections with shape.
			boolean intersects = intersectionWithShapeTest(points,connection, sharpestID, i);
			if(matchByAngle && !intersects)
			{
				Score score = new Score();
				//Add matching point
				matchingPointsIds.add(Integer.valueOf(i));
				///////////////////////////////////////SCORE////////////
				//4.3. Assign score to connections that will make this angle convex for both new shapes.
				if(relativeConnectionAngle>Math.PI && relativeConnectionAngle < Math.PI+outerAngle)
				{
					score.total += 100;
				}
				//4.4 Decrease score for very sharp angles
				score.total -= Math.max((1 - (relativeConnectionAngle-outerAngle))*5, 0);
				score.total -= Math.max((1 - (Math.PI*2 - relativeConnectionAngle))*5, 0);
				score.total -= Math.max((1 - getFullAngle(getLineFromIndex(points, i, false), reverseConnection))*5, 0);
				score.total -= Math.max((1 - getFullAngle(reverseConnection, getLineFromIndex(points, i, true)))*5, 0);
				//4.5. Calculate size
				score.length = lineSize(connection);
				//4.7. Assign score for connections with concave points.
				if(!isConvexPoint(getLineFromIndex(points, i, false), getLineFromIndex(points, i, true)))
				{
					score.total += 10;
					//4.6. Assign score for connections that will make convex angles on the opposite side.
					boolean ccwAngleConvex = isConvexPoint(getLineFromIndex(points, i, false), reverseConnection);
					boolean cwAngleconvex =  isConvexPoint(reverseConnection, getLineFromIndex(points, i, true));
					if(ccwAngleConvex && cwAngleconvex)
					{
						score.total += 100;
					}
				}
				////////////////////////////////////////////////////////
				matchingPointsScore.add(score);
			}
		}
		
		//4.7 Get max size to normalize size score
		float maxSize=0;
		for(Score score : matchingPointsScore)
		{
			if(score.length>maxSize)
			{
				maxSize = score.length;
			}
		}

		//5. Select the best connection
		int connectionId = matchingPointsIds.get(0);
		float maxScore = 0;
		for(int i = 0; i < matchingPointsIds.size; i++)
		{
			Score score = matchingPointsScore.get(i);
			float currentScore = score.total+(1-score.length/maxSize)*10;
			if(currentScore>maxScore)
			{
				connectionId = matchingPointsIds.get(i);
				maxScore = currentScore;
			}
		}
		//6. Split shapes 
		result1 = getSubShape(points, connectionId, sharpestID);
		result2 = getSubShape(points, sharpestID, connectionId);
		return true;
	}
	
	private float lineSize(float[] line)
	{
		float dx = line[0]-line[2];
		float dy = line[1]-line[3];
		return (float) Math.sqrt(dx*dx + dy*dy);
	}
	
	/**
	 * Return cyclic shape points from larger shape.
	 * @param point - array of points coordinates {x1,y1,x2,y2...}
	 * @param startId - start of new shape. Id of point (not of element in array)
	 * @param endId - end of new shape.
	 * @return subshape
	 */
	private float[] getSubShape(float[] point, int startId, int endId)
	{
		startId=startId*2;
		endId=endId*2;
		float[] result = null;
		if(startId<endId)
		{
			result = new float[endId-startId+2];
			for(int i = startId; i<=endId+1; i++)
			{
				result[i-startId] = point[i];
			}
		}
		if(startId>endId)
		{
			result = new float[point.length-startId+endId+2];
			for(int i = startId; i<point.length; i++)
			{
				result[i-startId] = point[i];
			}
			for(int i = 0; i<=endId+1; i++)
			{
				result[(point.length-startId)+i] = point[i];
			}
		}
		return result;
	}
	
	/**
	 * @param id  - id of point in array. Not element of array but point.
	 * @param offset - offset in the destination array
	 * @param points - source array
	 * @param point - destination array
	 */
	private void getPointById(int id, int offset, float[] points, float[] point)
	{
		point[offset] = points[id*2];
		point[offset+1] = (id*2==points.length-1) ? points[0]:points[id*2+1];
	}
	
	/**Check if point is convex for two lines of CCW shape.
	 * 
	 * @param first - line that looks forward from this point of CCW shape
	 * @param second - line that looks backward from this point of CCW shape
	 * @return
	 */
	private boolean isConvexPoint(float[] first, float[] second)
	{
		//Calculate vectors from lines.
		float x1 = first[2] - first[0];
		float y1 = first[3] - first[1];
		float x2 = second[2] - second[0];
		float y2 = second[3] - second[1];
		//Calculate Z coordinate of cross product {x1, y1, 0} x {x2, y2, 0}
		float z3 = x1*y2-x2*y1;
		return z3 > 0;
	}
	
	/** return angle between lines from 0 to PI */
	private float getAngle(float[] first, float[] second)
	{
		//Calculate vectors from lines.
		float x1 = first[2] - first[0];
		float y1 = first[3] - first[1];
		float x2 = second[2] - second[0];
		float y2 = second[3] - second[1];
		
		//calculate magnitude of vectors
		float size1 = (float) Math.sqrt(x1*x1 + y1*y1);
		float size2 = (float) Math.sqrt(x2*x2 + y2*y2);
		
		//Normalize
		x1 = x1/size1;
		y1 = y1/size1;
		x2 = x2/size2;
		y2 = y2/size2;
		
		//calculate dot product
		float dot = x1*x2+y1*y2;
		
		float angle = (float) Math.acos(dot);
		
		return angle;
	}
	
	/**return angle between lines. From 0 to 2*PI. Direction is CCW*/
	private float getFullAngle(float[] first, float[] second)
	{
		//Calculate vectors from lines.
		float x1 = first[2] - first[0];
		float y1 = first[3] - first[1];
		float x2 = second[2] - second[0];
		float y2 = second[3] - second[1];
		
		float angle = (float) (Math.atan2(y2, x2) - Math.atan2(y1, x1));
		if(angle<0)
		{
			angle += 2*Math.PI;
		}
		return angle;
	}
	
	/**
	 * Return true if CCW direction.
	 * @param pointsArr
	 * @return
	 */
	private boolean detectDirection(float[] pointsArr)
	{
		float minY = pointsArr[1];
		int bottomPointIndex = 0;
		for(int i = 0; i<pointsArr.length/2;i++)
		{
			if(pointsArr[i*2+1]<minY)
			{
				bottomPointIndex = i;
				minY = pointsArr[i*2+1];
			}
		}
		float bottomPointX = pointsArr[bottomPointIndex*2];
		float bottomPointY = pointsArr[bottomPointIndex*2+1];
		float prevPointX;
		float prevPointY;
		float nextPointX;
		float nextPointY;
		//find prev point
		if(bottomPointIndex==0)
		{
			prevPointX = pointsArr[pointsArr.length - 2];
			prevPointY = pointsArr[pointsArr.length - 1];
		}
		else
		{
			prevPointX = pointsArr[(bottomPointIndex-1)*2];
			prevPointY = pointsArr[(bottomPointIndex-1)*2 + 1];
		}
		//find next point
		if(bottomPointIndex==pointsArr.length/2 - 1)
		{
			nextPointX =  pointsArr[0];
			nextPointY =  pointsArr[1];
		}
		else
		{
			nextPointX = pointsArr[(bottomPointIndex+1)*2];
			nextPointY = pointsArr[(bottomPointIndex+1)*2+1];
		}
		
		float firstAngle = (float) Math.atan2(					
				prevPointY-bottomPointY,
				prevPointX-bottomPointX);
		float secondAngle = (float) Math.atan2(
				nextPointY-bottomPointY,
				nextPointX-bottomPointX);
		//Gdx.app.log("Convex decomposition", "Angle 1 = "+firstAngle+"; Angle 2 = "+secondAngle);
		if(firstAngle<0||secondAngle<0||firstAngle==secondAngle)
		{
			//Gdx.app.log("Direction detector", "Unexpected angles.");
		}
		if(firstAngle>secondAngle)
		{
			return true;
		}
		if(firstAngle<secondAngle)
		{
			return false;
		}
		return true;//This may cause bugs in rare cases. 
		//Anyway shapes with intersections should be found 
		//before passing this methods.
	}
}
