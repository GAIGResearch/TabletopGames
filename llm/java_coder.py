import os
import re
import subprocess

from openai import OpenAI

from config import api_key

# Set your API key
os.environ['OPENAI_API_KEY'] = api_key


def extract_code(response: str) -> str:
    # Use regex to extract code between ```java and ```
    code_match = re.search(r'```java(.*?)```', response, re.DOTALL)
    if code_match:
        return code_match.group(1).strip()
    return response


# Function to generate Python code using OpenAI API
def generate_python_code(prompt: str, model: str = "gpt-3.5-turbo") -> str:
    client = OpenAI(
        api_key=os.environ.get(api_key)
    )

    response = client.chat.completions.create(
        messages=[{"role": "user", "content": "You are a Java coding assistant."},
                  {"role": "user", "content": prompt}],
        model=model,
        temperature=0.7
    )

    # Extract the code part from the response
    code = response.choices[0].message.content
    return extract_code(code)


def run_jar(jar_path, args):
    # Construct the command to run the .jar file
    command = ["java", "-jar", jar_path] + args
    try:
        # Run the command and capture the output
        result = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True  # This will raise an exception if the command fails
        )
        # Return the standard output
        return result.stdout
    except subprocess.CalledProcessError as e:
        # Handle errors in the called process
        print(f"Error running jar: {e.stderr}")
        return e.stderr


def evaluate(prompt: str):
    # Generate code
    # generated_code = ""
    generated_code = generate_python_code(prompt)
    print("Generated Code:\n", generated_code)

    lines = generated_code.split('\n')

    # Write the prompt response to file
    with open(file_path, 'w') as file:
        for line in lines:
            stripped_line = line.lstrip()
            if not stripped_line.startswith('//'):
                # Write the line to the file
                file.write(line + '\n')

    # run Java code and extract win percentage from output
    jar_path = "llm.jar"
    # args = ["arg1", "arg2"]  # Replace with actual arguments for your jar
    args = []

    out = run_jar(jar_path, args)
    if "Error" in out or "error" in out:
        return -1,-1,out


    output = out.split("\n")[-2].split(',')
    wr = float(output[0])
    t = float(output[1])
    return wr, t, ""


# Output test file path
file_path = "llm/TicTacToeEvaluator.java"

# Initial task prompt
task_prompt = """
You are playing Tic Tac Toe.
Write a heuristic function within a TicTacToeEvaluator class, such that with any given game state and our player ID, we return a double value between 0 and 1. 
The value should be closer to 0 if we lose the game, and closer to 1 if we are closer to winning the game. 
Take into account the whole board position and possible opponent moves. 
The board is accessible from the game state object through the getGridBoard() method, which returns an object of type GridBoard<Token>. 
This object has functions to getWidth(), getHeight(), and getElement(x,y) which returns a Token object equal to "x" for player ID 0, and "o" for player ID 1. 
You can use new Token("x") to create token objects for any comparisons. 
The function should consider both situations when we are player ID 0 (starting the game), and player ID 1.

The function should be written in Java, return a double, with this signature: public double evaluateState(TicTacToeGameState gs, int playerId)
Write all the code I've asked for in a single function. Assume all the other classes are implemented, no need for a main function either. Just write the evaluation function.
Add all the import statements required, in addition to importing games.tictactoe.TicTacToeGameState, core.components.GridBoard and core.components.Token
Do not add any comments in the code. 
"""


win_rate, ties, error = evaluate(task_prompt)
execution_time = 1

# Iterate if necessary
iteration = 1
max_iters = 1
while win_rate + ties < 0.4 and iteration < max_iters:  # Set your performance threshold
    #print(f"\nIteration {iteration}: Providing feedback and requesting optimization...\n")
    feedback_prompt = f"""
    The initial implementation of the heuristic needs improvements.
    Please optimize the code for better performance. Aim for higher ties or wins. Here are the results:

    Wins: {win_rate:.2f}.
    Ties: {ties:.2f}.
    """
    if error:
        feedback_prompt = f"Remove comments from the code. Compilation error, fix it: {error}"

    win_rate, ties, error = evaluate(feedback_prompt)
    optimized_code = generate_python_code(feedback_prompt)

    iteration += 1

print(f"\nFinished! Final results: {win_rate:.2f} wins and {ties:.2f} ties.")
