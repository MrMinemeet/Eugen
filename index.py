import json
import sys
import random

if sys.version_info < (3, 8):
    exit("Python 3.8 or greater required!")

import discord


class EugenBot(discord.Client):
    def __init__(self, *, intend: discord.Intents):
        activity = discord.Activity(name='💰 💸 💵', type=discord.ActivityType.watching)
        super().__init__(intents=intend, activity=activity)
        self.tree = discord.app_commands.CommandTree(self)

    async def setup_hook(self) -> None:
        await self.tree.sync()


if __name__ == "__main__":

    # Try loading config from file if existing
    try:
        with open("config.json", 'r', encoding='utf-8') as config_file:
            config = json.load(config_file)
    except FileNotFoundError as _:
        config = {}
        pass
    except json.JSONDecodeError as ex:
        print("Something went wrong when loading 'config.json. Error:")
        print(ex)
        config = {}

    if config.get("token", None) is None:
        # No token found in config
        config["token"] = input("Please enter the bot's token: ").strip()

    if config.get("log_channel", None) is None:
        # No log channel found in config
        config["log_channel"] = int(input("Please enter the log channel's ID: ").strip())

    # Store updated config
    with open("config.json", "w+", encoding="utf-8") as config_file:
        json.dump(config, config_file, indent=4)

    intents = discord.Intents.default()
    intents.message_content = True
    client = EugenBot(intend=intents)


    @client.event
    async def on_ready():
        print(f"Logged in as {client.user} (ID: {client.user.id})")

        startup_message = random.choice(
            ["Well, if I was a robot, which I'm not, at least I'm well put together.", "Hello! I Like Money.",
             "A 5 letter word for happiness - MONEY."])
        await log(startup_message, True)


    async def log(log_message: str, channel_only: bool = False):
        LOG_CHANNEL = client.get_channel(config["log_channel"])
        if not channel_only:
            print(log_message)
        await LOG_CHANNEL.send(log_message)


    @client.tree.command(name="ping", description="Just checks if commands work. Answers with a pong")
    async def ping(interaction: discord.Interaction):
        await log(f"{interaction.user} used the 'ping' command")
        await interaction.response.send_message("pong!")


    @client.tree.command(name="whoami", description="Get some information about yourself")
    async def whoami(interaction: discord.Interaction):
        await interaction.response.send_message(
            f"You are {interaction.user} sending a message from '{interaction.user.guild}' in "
            f"'{interaction.channel.name}'. Your top most role is {interaction.user.top_role} and you joined this "
            f"server on {interaction.user.joined_at}. You got {len(interaction.user.roles)} roles in total."
        )


    @client.tree.command(name="shutdown", description="Eugen will go to sleep. If my master tells me so")
    async def shutdown(interaction: discord.Interaction):
        # Check if it is owner (me at the moment)
        if str(interaction.user) != 'MrMinemeet#0815':
            await interaction.response.send_message("You are not my master, I only fight for the mighty moby dollar!")
        else:
            await log("Shutting down...", True)
            shutdown_message = random.choice(
                ["You know what they say: \"If it ain't broke, don't fix it.\" ~ And I'm broken",
                 "What doesn't kill you, usually succeeds in the second attempt. ~ This is the second time"])
            await interaction.response.send_message(shutdown_message)
            await client.close()


    client.run(config["token"], log_handler=None)
