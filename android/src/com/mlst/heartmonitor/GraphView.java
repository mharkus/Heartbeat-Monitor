package com.mlst.heartmonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class GraphView extends View {

    private Paint graphPaint;
    private Paint pointerPaint;
    private Paint signalPaint;
    private int len;
    private int degree = 4;
    private boolean drawPulse;
    private int ctr;

    public GraphView(Context context, AttributeSet attrs) {
	super(context, attrs);

	init();
    }

    private void init() {
	graphPaint = new Paint();
	graphPaint.setColor(Color.GREEN);

	pointerPaint = new Paint();
	pointerPaint.setColor(Color.parseColor("#00CC33"));
	pointerPaint.setStrokeWidth(10);
	pointerPaint.setAlpha(100);

	signalPaint = new Paint();
	signalPaint.setColor(Color.parseColor("#00CC33"));
	signalPaint.setStrokeWidth(10);
	signalPaint.setStyle(Style.FILL_AND_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
	super.onDraw(canvas);
	
	drawGraph(canvas);
    }

    private void drawGraph(Canvas canvas) {
	// horizontal lines
	for (int i = 0; i < 10; i++) {
	    canvas.drawLine(0, 25 + (i * 60), getWidth(), 25 + (i * 60), graphPaint);
	}

	// vert lines
	for (int i = 0; i < 12; i++) {
	    canvas.drawLine(25 + (i * 60), 0, 25 + (i * 60), getHeight(), graphPaint);
	}
	
	if(degree == 5){
	    ctr += 28;
	    canvas.drawCircle(len, 25 + (4 * 60) + ctr, 10, signalPaint);
	    if(ctr >= 100){
		ctr = 0;
	    }
	}else if(degree == 2){
	    ctr += 25;
	    canvas.drawCircle(len, 25 + (5 * 60) - ctr, 10, signalPaint);
	    if(ctr >= 200){
		ctr = 0;
	    }
	}else{
	    canvas.drawCircle(len, 25 + (degree * 60), 10, signalPaint);
	}

	    switch(degree){
	    	case 2:{
	    	    if(ctr != 0){
			break;
		    }
	    	    degree = 6;
	    	    break;
	    	}
	    	case 3:{
	    	    degree = 4;
	    	    break;
	    	}
		case 4:{
		    if(drawPulse){
			degree = 5;	
		    }
		    
		    break;
		}
		case 5:{
		    if(ctr != 0){
			break;
		    }
		    
		   
		    drawPulse = false;
		    degree = 2;
		    
		    break;
		}
		case 6:{
		    degree = 3;
		}
	    }
	    
	
	len += 5;

	if (len >= getWidth()) {
	    len = 0;
	    degree = 4;
	    drawPulse = false;
	}

	invalidate();

    }

    public boolean isDrawPulse() {
        return drawPulse;
    }

    public void setDrawPulse(boolean drawPulse) {
        this.drawPulse = drawPulse;
    }

 

}
