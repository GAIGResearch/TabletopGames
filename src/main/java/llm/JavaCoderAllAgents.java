package llm;

import java.io.IOException;

public class JavaCoderAllAgents {

    public static void main(String[] args) throws IOException {
        // we run JavaCoder for the specified args with each model type and size

        for (LLMAccess.LLM_SIZE size : LLMAccess.LLM_SIZE.values()) {
            //   LLMAccess.LLM_SIZE size = LLMAccess.LLM_SIZE.LARGE;
            for (LLMAccess.LLM_MODEL model : LLMAccess.LLM_MODEL.values()) {
                //     LLMAccess.LLM_MODEL model = LLMAccess.LLM_MODEL.ANTHROPIC;
                if (model == LLMAccess.LLM_MODEL.LLAMA) {
                    continue;
                }
                String[] newArgs = new String[args.length + 2];
                System.arraycopy(args, 0, newArgs, 0, args.length);
                newArgs[args.length] = "model=" + model.toString();
                newArgs[args.length + 1] = "size=" + size.toString();
                JavaCoder.main(newArgs);
            }
        }
    }
}
