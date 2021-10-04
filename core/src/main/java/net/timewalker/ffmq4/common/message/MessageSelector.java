/*
 * This file is part of FFMQ.
 *
 * FFMQ is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * FFMQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FFMQ; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.timewalker.ffmq4.common.message;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;

import net.timewalker.ffmq4.common.message.selector.MessageSelectorParser;
import net.timewalker.ffmq4.common.message.selector.SelectorIndexKey;
import net.timewalker.ffmq4.common.message.selector.expression.Identifier;
import net.timewalker.ffmq4.common.message.selector.expression.SelectorNode;
import net.timewalker.ffmq4.common.message.selector.expression.literal.Literal;
import net.timewalker.ffmq4.common.message.selector.expression.literal.StringLiteral;
import net.timewalker.ffmq4.common.message.selector.expression.literal.StringLiteralList;
import net.timewalker.ffmq4.common.message.selector.expression.operator.AndOperator;
import net.timewalker.ffmq4.common.message.selector.expression.operator.EqualsOperator;
import net.timewalker.ffmq4.common.message.selector.expression.operator.InOperator;

/**
 * <p>Object implementation of a JMS message selector</p>
 */
public final class MessageSelector
{
    // Parsed selector node tree
    private SelectorNode selectorTree;
    
    /**
     * Constructor
     */
    public MessageSelector( String selectorString ) throws JMSException
    {
        this.selectorTree = new MessageSelectorParser(selectorString).parse();
    }

    /**
     * Test the selector against a given message
     */
    public boolean matches( Message message ) throws JMSException
    {
        Boolean result = selectorTree != null ? selectorTree.evaluateBoolean(message) : null;
        return result != null && result.booleanValue();
    }
    
    public List<SelectorIndexKey> getIndexableKeys()
	{
    	if (selectorTree == null)
    		return null;
    	
    	return findIndexableKeys(selectorTree, null);
	}
    
    private List<SelectorIndexKey> findIndexableKeys( SelectorNode node , List<SelectorIndexKey> keys )
	{
    	if (node instanceof EqualsOperator)
    	{
    		EqualsOperator eq = (EqualsOperator)node;
    		if (eq.leftOperand() instanceof Identifier && eq.rightOperand() instanceof Literal)
    		{
    			Identifier id = (Identifier)eq.leftOperand();
    			Literal value = (Literal)eq.rightOperand();
    			if (keys == null)
    				keys = new ArrayList<>();
    			keys.add(new SelectorIndexKey(id.getName(), value.getValue()));
    		}
    		else
			if (eq.leftOperand() instanceof Literal && eq.rightOperand() instanceof Identifier)
    		{
    			Identifier id = (Identifier)eq.rightOperand();
    			Literal value = (Literal)eq.leftOperand();
    			if (keys == null)
    				keys = new ArrayList<>();
    			keys.add(new SelectorIndexKey(id.getName(), value.getValue()));
    		}
    	}
    	else
		if (node instanceof InOperator)
    	{
			InOperator inOperator = (InOperator)node;
			if (inOperator.leftOperand() instanceof Identifier && inOperator.rightOperand() instanceof StringLiteralList)
    		{
    			Identifier id = (Identifier)inOperator.leftOperand();
    			StringLiteralList valueList = (StringLiteralList)inOperator.rightOperand();
    			if (keys == null)
    				keys = new ArrayList<>();
    			
    			// Only keep distinct values, otherwise the subscriber may receive several times the same message
    			List<String> distinctValues = new ArrayList<>();
    			SelectorNode[] items = valueList.getItems();
    	        for (int n = 0 ; n < items.length ; n++)
    	        {
    	        	String value = ((StringLiteral)items[n]).getValue().toString();
    	        	if (!distinctValues.contains(value))
    	        		distinctValues.add(value);
    	        }
    	        Object[] values = distinctValues.toArray(new Object[distinctValues.size()]) ;
    			keys.add(new SelectorIndexKey(id.getName(), values));
    		}
    	}
    	if (node instanceof AndOperator)
    	{
    		AndOperator and = (AndOperator)node;
    		// Recursion
    		keys = findIndexableKeys(and.leftOperand(),keys);
    		keys = findIndexableKeys(and.rightOperand(),keys);
    	}
    	
    	return keys;
	}
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString()
    {
        return selectorTree != null ? selectorTree.toString() : "";
    }
}
