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
            # Split the line at the first occurrence of "//"
            cleaned_line = line.split('//', 1)[0]
            file.write(cleaned_line.rstrip() + '\n')
            # if not stripped_line.startswith('//'):
            #     # Write the line to the file
            #     file.write(line + '\n')

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
Write a java class called TicTacToeEvaluator class, with only a single function with this signature: 
 - public double evaluateState(TicTacToeGameState gs, int playerId)
Then, write the contents of this function. This is a heuristic function to play Tic Tac Toe, so that with any given game 
state and our player ID, we return a double value between 0 and 1. The value should be closer to 0 if we lose the game, 
and closer to 1 if we are closer to winning the game.  The function should consider both situations when we are 
player ID 0 (starting the game), and player ID 1. 
Take into account the whole board position and possible opponent moves. 
Write all the code I've asked for in a single function. Assume all the other classes are implemented, no need for a main function either. Just write the evaluation function.
Add all the import statements required, in addition to importing games.tictactoe.TicTacToeGameState, core.components.GridBoard and core.components.Token
Do not add any comments in the code. 
You can use the following API:
 - GridBoard<Token> getGridBoard(), to access the board of the game.
 - GridBoard has the following functions you can also use:
   - int getWidth(), to return the width of the board.
   - int getHeight(), to return the height of the board.
   - Token getElement(int x, int y), that returns the Token on the position of the board with row x and column y. 
 - Token represents a piece placed by a player. 
   - With player the token belongs to is represented with a string. This string is "x" for player ID 0, and "o" for player ID 1. 
   - Token(String) allows your to create token objects for any comparisons. 
   - String getTokenType() returns the string representation of the token type.
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
