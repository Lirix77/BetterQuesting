package adv_director.rw2.api.d_script.operators.binary;

import adv_director.rw2.api.d_script.IExpression;
import adv_director.rw2.api.d_script.ScriptScope;

public class OperatorShiftRight implements IExpression<Number>
{
	private final IExpression<Number> e1;
	private final IExpression<Number> e2;
	
	public OperatorShiftRight(IExpression<Number> e1, IExpression<Number> e2)
	{
		this.e1 = e1;
		this.e2 = e2;
	}
	
	@Override
	public Number eval(ScriptScope scope) throws Exception
	{
		return e1.eval(scope).longValue() >> e2.eval(scope).longValue();
	}

	@Override
	public Class<Number> type()
	{
		return Number.class;
	}
	
}