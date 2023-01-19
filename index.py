import json
import sys

if sys.version_info < (3, 8):
    exit("Python 3.8 or greater required!")

import discord


class EugenBot(discord.Client):
    def __init__(self, *, intents: discord.Intents):
        super().__init__(intents=intents)
        self.tree = discord.app_commands.CommandTree(self)

    async def setup_hook(self) -> None:
        await self.tree.sync()


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
client = EugenBot(intents=intents)


@client.event
async def on_ready():
    print(f"Logged in as {client.user} (ID: {client.user.id})")


@client.tree.command(name="ping", description="Just checks if commands work. Answers with a pong")
async def ping(interaction: discord.Interaction):
    print(f"{interaction.user} used 'ping' command.")

    await interaction.response.send_message("pong!")


@client.tree.command(name="whoami", description="Get some information about yourself")
async def whoami(interaction: discord.Interaction):
    print(f"{interaction.user} used 'whoami' command.")

    await interaction.response.send_message(f"You are {interaction.user} sending a message from "
                                            f"{interaction.user.guild} in {interaction.user.channel}. Your top most "
                                            f"role is {interaction.user.top_role} and you joined this server on"
                                            f" {interaction.user.joined_at}"
                                            )


@client.tree.command(name="shutdown", description="Eugen will go to sleep. If my master tells me so")
async def shutdown(interaction: discord.Interaction):
    print(f"{interaction.user} used 'whoami' command.")
    # Check if it is owner (me at the moment)
    if str(interaction.user) != 'MrMinemeet#0815':
        await interaction.response.send_message("You are not my master!")
    else:
        print("Master requested a shutdown")
        await interaction.response.send_message("Ok master. I'll rest now")
        await client.close()


client.run(config["token"], log_handler=None)
