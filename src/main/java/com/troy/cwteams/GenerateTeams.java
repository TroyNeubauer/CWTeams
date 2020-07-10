package com.troy.cwteams;

import com.google.common.primitives.UnsignedInts;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateTeams
{


	public static void gen(List<CWPlayer> players, double maxDev, int limitOutput, int teamCount)
	{
		int[] teamSizes = new int[teamCount];
		for (int i = 0; i < players.size(); i++)
		{
			teamSizes[i % teamSizes.length]++;
		}

		{
			StringBuilder sb = new StringBuilder();
			sb.append("Match is set for: ");
			for (int i = 0; i < teamSizes.length; i++)
			{
				sb.append(teamSizes[i]);
				if ( i < teamSizes.length - 1)
				{
					sb.append(" v ");
				}
			}
			Main.info(sb.toString());
		}
		OptionalDouble optionalAverage = players.stream().map(CWPlayer::getOverall).mapToDouble(Double::doubleValue).average();
		if (!optionalAverage.isPresent())
		{
			Main.error("Players stream is empty!");
			System.exit(1);
		}
		double averageTeamSize = (double) players.size() / (double) teamCount;
		double averageTeamRaking = optionalAverage.getAsDouble() * averageTeamSize;
		Main.info("Average team size is " + averageTeamSize + ". average team rating is " + averageTeamRaking + " +-" + maxDev);


		//The indices of tempPlayers correspond to the indices of players inside the players list
		//Jumps of indices according to the values in teamSizes indicate teams
		//IE if teamSizes = {2, 3, 2} then the first 2 indices in tempPlayers are on team #1 the next indices in tempPlayers are on team #2 etc
		//This is done in a single 1d array to improve cache locality and thus performance
		int[] tempPlayers = new int[players.size()];
		for (int i = 0; i < tempPlayers.length; i++)
		{
			//Fill in the most basic team mapping
			tempPlayers[i] = i;
		}

		//This set contains hashes of the team combos that we already tried so that we don't repeat
		HashSet<Long> combinationsTried = new HashSet<Long>();
		long start = System.currentTimeMillis();
		long lastOption = 0;
		long comboCount = 0;
		for (int validOptions = 0; validOptions < limitOutput; )
		{
			shuffel(tempPlayers);
			comboCount++;
			long singleStart = System.currentTimeMillis();
			final long TIMEOUT = 15 * 1000;
			while (!areTeamsValid(players, tempPlayers, teamSizes, averageTeamRaking, maxDev))
			{
				if (System.currentTimeMillis() - singleStart > TIMEOUT)
				{
					Main.error("Failed to find team combination after " + (TIMEOUT / 1000) + " seconds! Tried " + comboCount + " combinations to no avail");
					System.exit(1);
				}
				shuffel(tempPlayers);
				comboCount++;
			}
			long hash = getTeamsHash(players, tempPlayers, teamSizes);
			if (!combinationsTried.contains(hash))
			{
				//We found a valid configuration
				lastOption = System.currentTimeMillis();
				validOptions++;
				combinationsTried.add(hash);
				System.out.println("TEAMS:");
				int playerIndex = 0;
				int teamIndex = 0;
				while (playerIndex < tempPlayers.length)
				{
					System.out.println("\tTeam #" + (teamIndex + 1));
					int teamSize = teamSizes[teamIndex++];
					double teamSum = 0.0;
					for (int j = 0; j < teamSize; j++)
					{
						CWPlayer player = players.get(tempPlayers[playerIndex++]);
						System.out.println("\t\t" + player.realName + " (" + player.username + ")");
						teamSum += player.getOverall();
					}

					System.out.println("\tTEAM Strength: " + teamSum + "\n");
				}
			}
			else
			{
				//Check for timeout in case we already found all possible teams
				if (lastOption != 0 && (System.currentTimeMillis() - lastOption) > TIMEOUT)
				{
					Main.warn("Failed to more combinations after " + (TIMEOUT / 1000) + " seconds! Low search space? Exiting!");
					break;
				}

			}
		}

		double seconds = (System.currentTimeMillis() - start) / 1000.0;
		Main.info("Generated " + combinationsTried.size() + " valid team possibilities in " + NumberFormat.getInstance().format(seconds) + " seconds");
		Main.info("That's " + NumberFormat.getInstance().format(comboCount / seconds) + " configurations/second (" + NumberFormat.getInstance().format(seconds / comboCount * 1000_000_000.0) + " nano seconds / configuration) evaluated");
		Main.info("Evaluated " + NumberFormat.getInstance().format(comboCount) + " possible configurations");
	}

	private static Long getTeamsHash(List<CWPlayer> players, int[] tempPlayers, int[] teamSizes)
	{
		//Sort the hashes by value so that teams with players in a different order are still considered the same team
		long[] teamHashes = new long[teamSizes.length];
		int i = 0;
		int teamIndex = 0;
		while (i < tempPlayers.length)
		{
			int teamSize = teamSizes[teamIndex];
			int[] individualHashes = new int[teamSize];
			for (int j = 0; j < teamSize; j++)
			{
				individualHashes[j] = players.get(tempPlayers[i++]).hashCode();
			}
			Arrays.sort(individualHashes);
			long hash = 0;
			for (int j = 0; j < teamSize; j++)
			{
				int shift;
				if (teamSize == 1)
					shift = 0;
				else//Increase the shift each iteration so that we get a juicy 64 bit hash for a team in the end
					shift = 32 * j / (teamSize - 1);
				hash ^= ((long) individualHashes[j]) << shift;
			}
			teamHashes[teamIndex] = hash;
			teamIndex++;
		}
		Arrays.sort(teamHashes);

		//Copied from Arrays.hashCode and extended to 64 bits
		long result = 1;
		for(int ii = 0; ii < teamHashes.length; ii++)
		{
			long val = teamHashes[ii];
			result = 31 * result + val ^ val >>> 32;
		}

		return result;

	}

	private static boolean areTeamsValid(List<CWPlayer> players, int[] tempPlayers, int[] teamSizes, double neededAverage, double maxDev)
	{
		int i = 0;
		int teamIndex = 0;
		while (i < tempPlayers.length)
		{
			int teamSize = teamSizes[teamIndex++];
			double teamSum = 0.0;
			for (int j = 0; j < teamSize; j++)
			{
				teamSum += players.get(tempPlayers[i++]).getOverall();
			}
			//Make sure this team is within range of the max deviation
			if (Math.abs(neededAverage - teamSum) > maxDev)
			{
				//This team is too good or too bad...
				return false;
			}
		}

		return true;
	}


	private static Random rand = new Random();

	private static void shuffel(int[] array)
	{
		for (int i = 0; i < array.length; i++)
		{
			int randomIndexToSwap = rand.nextInt(array.length);
			int temp = array[randomIndexToSwap];
			array[randomIndexToSwap] = array[i];
			array[i] = temp;
		}
	}

}
