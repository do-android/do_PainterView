package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_PainterView_MAbstract extends DoUIModule{

	protected do_PainterView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("brushColor", PropertyDataType.String, "FF0000FF", false));
		this.registProperty(new DoProperty("brushWidth", PropertyDataType.Number, "3", false));
	}
}