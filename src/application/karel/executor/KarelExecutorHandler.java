/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application.karel.executor;

import application.karel.KarelEnvironment;
import application.karel.KarelExecutor;
import application.karel.ExecutionListener;
import com.instil.lgi.Block;

import java.util.Map.Entry;
import java.util.Stack;

/**
 *
 * @author Administrador
 */
public interface KarelExecutorHandler {

    public Block compile(Block b);

    public void set_values( KarelExecutor __ka_exec,
                            KarelEnvironment __ka_env,
                            ExecutionListener __ka_exe_lis
                            );
    public void execute( Stack<Entry<Block,Integer>> execStack, Stack<Object> retStack );
}
