package doext.implement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIBitmap;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoUIModule;
import doext.define.do_PainterView_IMethod;
import doext.define.do_PainterView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,do_PainterView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_PainterView_View extends View implements DoIUIModuleView, do_PainterView_IMethod {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_PainterView_MAbstract model;

	private Canvas mCanvas;
	private Paint mBitmapPaint;
	private Bitmap mBitmap;

	private ArrayList<DrawPath> savePath;
	private ArrayList<DrawPath> deletePath;

	private float mX, mY;
	private int bitmapWidth;
	private int bitmapHeight;

	private int bgColor = Color.TRANSPARENT;

	private DrawPath mDrawPath;

	private int mPaintColor = Color.parseColor("#FF0000");
	private float mPaintWidth = 3;

	private class DrawPath {
		Path mPath;
		Paint mPaint;
	}

	public do_PainterView_View(Context context) {
		super(context);

		savePath = new ArrayList<DrawPath>();
		deletePath = new ArrayList<DrawPath>();
	}

	private Paint createPaint() {
		Paint mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setColor(this.mPaintColor);
		mPaint.setStrokeWidth(this.mPaintWidth);
		return mPaint;
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_PainterView_MAbstract) _doUIModule;
		bitmapWidth = (int) this.model.getRealWidth();
		bitmapHeight = (int) this.model.getRealHeight();
		initCanvas();
	}

	//初始化画布
	private void initCanvas() {
		//画布大小 
		mBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap); //所有mCanvas画的东西都被保存在了mBitmap中
		mCanvas.drawColor(bgColor);
		mDrawPath = new DrawPath();

		mBitmapPaint = new Paint(Paint.DITHER_FLAG);

	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("brushColor")) {
			String _brushColor = _changedValues.get("brushColor");
			this.mPaintColor = DoUIModuleHelper.getColorFromString(_brushColor, Color.BLACK);
		}
		if (_changedValues.containsKey("brushWidth")) {
			String _brushWidth = _changedValues.get("brushWidth");
			this.mPaintWidth = DoTextHelper.strToFloat(_brushWidth, 3);
		}

		if (_changedValues.containsKey("bgColor")) {
			bgColor = DoUIModuleHelper.getColorFromString(_changedValues.get("bgColor"), Color.TRANSPARENT);
			mCanvas.drawColor(bgColor);
		}

	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("undo".equals(_methodName)) {
			this.undo(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("clear".equals(_methodName)) {
			this.clear(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}

		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @throws Exception
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("saveAsBitmap".equals(_methodName)) {
			this.saveAsBitmap(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		if ("saveAsImage".equals(_methodName)) {
			this.saveAsImage(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		removeAllPaint();
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 清空画板；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void clear(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		removeAllPaint();
	}

	/**
	 * 保存为Bitmap；
	 * 
	 * @throws Exception
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void saveAsBitmap(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _address = DoJsonHelper.getString(_dictParas, "bitmap", "");
		if (_address == null || _address.length() <= 0)
			throw new Exception("bitmap参数不能为空！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null)
			throw new Exception("bitmap参数无效！");
		if (_multitonModule instanceof DoIBitmap) {
			DoIBitmap _bitmap = (DoIBitmap) _multitonModule;
			_bitmap.setData(mBitmap);
			DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
			_scriptEngine.callback(_callbackFuncName, _invokeResult);
		}

	}

	/**
	 * 保存为图片；
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void saveAsImage(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws IOException, JSONException {
		String _format = DoJsonHelper.getString(_dictParas, "format", "JPEG");
		int _quality = DoJsonHelper.getInt(_dictParas, "quality", 100);
		String _outPath = DoJsonHelper.getString(_dictParas, "outPath", "");
		boolean _isUseDefault = false;
		if (_quality < 0 || _quality > 100) {
			_quality = 100;
		}
		CompressFormat _cFormat = CompressFormat.JPEG;
		String _fileName = DoTextHelper.getTimestampStr() + ".jpg.do";
		if ("PNG".equalsIgnoreCase(_format)) {
			_cFormat = CompressFormat.PNG;
			_fileName = DoTextHelper.getTimestampStr() + ".png.do";
		}
		String _fillPath = "";
		try {
			_fillPath = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentPage().getCurrentApp(), _outPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (TextUtils.isEmpty(_fillPath)) {
			_isUseDefault = true;
			_fillPath = _scriptEngine.getCurrentApp().getDataFS().getRootPath() + "/temp/do_PainterView/" + _fileName;
		}

		File _outFile = new File(_fillPath);
		if (!DoIOHelper.existFile(_fillPath)) {
			DoIOHelper.createFile(_fillPath);
		}
		OutputStream _outputStream = new FileOutputStream(_outFile);
		boolean _result = this.mBitmap.compress(_cFormat, _quality, _outputStream);
		_outputStream.close();

		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());

		String _resultText = "";
		if (_result) {
			if (_isUseDefault) {
				_resultText = "data://temp/do_PainterView/" + _fileName;
			} else {
				_resultText = _outPath;
			}
		}
		_invokeResult.setResultText(_resultText);
		_scriptEngine.callback(_callbackFuncName, _invokeResult);
	}

	/**
	 * 回退操作；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void undo(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		undo();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint); //显示旧的画布       
		if (mDrawPath.mPath != null) {
			// 实时的显示
			canvas.drawPath(mDrawPath.mPath, mDrawPath.mPaint);
		}
	}

	/**
	 * 撤销的核心思想就是将画布清空， 将保存下来的Path路径最后一个移除掉， 重新将路径画在画布上面。
	 */
	private void undo() {
		if (savePath != null && savePath.size() > 0) {
			//调用初始化画布函数以清空画布
			initCanvas();
			//将路径保存列表中的最后一个元素删除 ,并将其保存在路径删除列表中
			deletePath.add(savePath.get(savePath.size() - 1));
			savePath.remove(savePath.size() - 1);

			//将路径保存列表中的路径重绘在画布上
			Iterator<DrawPath> iter = savePath.iterator(); //重复保存
			while (iter.hasNext()) {
				DrawPath mDrawPath = iter.next();
				mCanvas.drawPath(mDrawPath.mPath, mDrawPath.mPaint);
			}
			invalidate();// 刷新
		}
	}

	/**
	 * 恢复的核心思想就是将撤销的路径保存到另外一个列表里面(栈)， 然后从redo的列表里面取出最顶端对象， 画在画布上面即可
	 */
	protected void redo() {
		if (deletePath.size() > 0) {
			//将删除的路径列表中的最后一个，也就是最顶端路径取出（栈）,并加入路径保存列表中
			DrawPath mDrawPath = deletePath.get(deletePath.size() - 1);
			savePath.add(mDrawPath);
			//将取出的路径重绘在画布上
			mCanvas.drawPath(mDrawPath.mPath, mDrawPath.mPaint);
			//将该路径从删除的路径列表中去除
			deletePath.remove(deletePath.size() - 1);
			invalidate();
		}
	}

	/*
	 * 清空的主要思想就是初始化画布 将保存路径的两个List清空
	 */
	private void removeAllPaint() {
		//调用初始化画布函数以清空画布
		initCanvas();
		invalidate();//刷新
		savePath.clear();
		deletePath.clear();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDrawPath = new DrawPath();
			Path mPath = new Path();
			mDrawPath.mPaint = createPaint();
			mDrawPath.mPath = mPath;
			mPath.reset();//清空path
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
			invalidate(); //清屏
			break;
		case MotionEvent.ACTION_MOVE:
			mDrawPath.mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			mDrawPath.mPath.lineTo(mX, mY);
			mCanvas.drawPath(mDrawPath.mPath, mDrawPath.mPaint);
			savePath.add(mDrawPath);
			mPath = null;
			invalidate();
			break;
		}
		return true;
	}

}