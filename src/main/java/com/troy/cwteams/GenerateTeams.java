package com.troy.cwteams;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.*;

public class GenerateTeams
{

	private static long teamValueFailedCount = 0, playerRestrictionsFailedCount = 0;
	private static List<int[]> teamResults = new ArrayList<int[]>();

	public static void gen(List<CWPlayer> players, List<PlayerRestrictor.PlayerRestriction> restrictions, double maxDev, int limitOutput, int teamCount, PrintStream output, boolean sort, int timeoutSeconds)
	{
		final long TIMEOUT = timeoutSeconds * 1000;
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
			Main.fatal("Players stream is empty!");
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

		//Initialize counters to 0
		long comboCount = 0;
		teamValueFailedCount = 0;
		playerRestrictionsFailedCount = 0;
		Main.info("Searching for teams... this may take a while");
		for (int validOptions = 0; validOptions < limitOutput; )
		{
			shuffel(tempPlayers);
			comboCount++;
			long singleStart = System.currentTimeMillis();
			while (!areTeamsValid(players, restrictions, tempPlayers, teamSizes, averageTeamRaking, maxDev))
			{
				if (System.currentTimeMillis() - singleStart > TIMEOUT)
				{
					printResults(output, players, teamSizes);
					Main.fatal("Failed to find more team combination after " + (TIMEOUT / 1000) + " seconds! Tried " + comboCount + " combinations to no avail");
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
				if (sort)
				{
					teamResults.add(Arrays.copyOf(tempPlayers, tempPlayers.length));
				}
				else
				{
					printTeam(players, tempPlayers, teamSizes, output, validOptions);
				}

			}
			else
			{
				//Check for timeout in case we already found all possible teams
				if (lastOption != 0 && (System.currentTimeMillis() - lastOption) > TIMEOUT)
				{
					Main.warn("Failed to find more team combinations after " + (TIMEOUT / 1000) + " seconds! Low search space? Exiting!");
					break;
				}

			}
		}

		printResults(output, players, teamSizes);
		double seconds = (System.currentTimeMillis() - start) / 1000.0;
		Main.success("Generated " + combinationsTried.size() + " valid team possibilities in " + NumberFormat.getInstance().format(seconds) + " seconds");
		Main.success("Evaluated " + NumberFormat.getInstance().format(comboCount) + " possible configurations");
		Main.success("That's " + NumberFormat.getInstance().format(comboCount / seconds) + " configurations/second (" + NumberFormat.getInstance().format(seconds / comboCount * 1000_000_000.0) + " nano seconds / configuration) evaluated");
		Main.success("Of the " + NumberFormat.getInstance().format(comboCount) + " attempted configurations, " +
				NumberFormat.getInstance().format(teamValueFailedCount) + " had a value out of range, and " +
				NumberFormat.getInstance().format(playerRestrictionsFailedCount) + " failed the restriction requirements");
	}

	private static void printResults(PrintStream output, List<CWPlayer> players, int[] teamSizes)
	{
		teamResults.sort(new Comparator<int[]>() {
			@Override
			public int compare(int[] a, int[] b) {
				double result = getTeamsDeltaStrength(players, a, teamSizes) - getTeamsDeltaStrength(players, b, teamSizes);
				if (result == 0.0)
					return 0;
				else if (result > 0.0)
					return -1;//Invert the sign so we sort largest to smallest
				else
					return 1;
			}
		});
		int i = 0;
		for (int[] tempPlayers : teamResults)
		{
			printTeam(players, tempPlayers, teamSizes, output, teamResults.size() - i++);
		}
	}

	public static double getTeamStrength(List<CWPlayer> players, int[] tempPlayers, int playerIndex, int teamCount)
	{
		double teamStrength = 0.0;
		for (int i = playerIndex; i < playerIndex + teamCount; i++)
		{
			CWPlayer player = players.get(tempPlayers[i]);
			teamStrength += player.getOverall();
		}
		return teamStrength;
	}

	public static double getTeamsDeltaStrength(List<CWPlayer> players, int[] tempPlayers, int[] teamSizes)
	{
		int playerIndex = 0;
		int teamIndex = 0;
		double minStrength = 0.0, maxStrength = 0.0;
		while (playerIndex < tempPlayers.length)
		{
			int teamSize = teamSizes[teamIndex++];
			double teamStrength = getTeamStrength(players, tempPlayers, playerIndex, teamSize);
			playerIndex += teamSize;

			if (minStrength == 0.0 || teamStrength < minStrength)
			{
				minStrength = teamStrength;
			}
			if (maxStrength == 0.0 || teamStrength > maxStrength)
			{
				maxStrength = teamStrength;
			}
		}
		return maxStrength - minStrength;
	}


	private static void printTeam(List<CWPlayer> players, int[] tempPlayers, int[] teamSizes, PrintStream output, int teamSetNum)
	{
		output.println("\nTEAM-SET #" + teamSetNum + " - delta: " + NumberFormat.getInstance().format(getTeamsDeltaStrength(players, tempPlayers, teamSizes)));
		int playerIndex = 0;
		int teamIndex = 0;
		while (playerIndex < tempPlayers.length)
		{
			int teamSize = teamSizes[teamIndex++];
			output.println("\tTeam #" + teamIndex + " strength " + NumberFormat.getInstance().format(getTeamStrength(players, tempPlayers, playerIndex, teamSize)));
			int[] ordering = new int[teamSize];
			for (int i = 0; i < ordering.length; i++)
			{
				ordering[i] = tempPlayers[playerIndex + i];
			}
			playerIndex += teamSize;
			ordering = Arrays.stream(ordering).
					boxed().
					sorted(Comparator.comparing(players::get)). // sort ascending
					mapToInt(i -> i).
					toArray();

			//Go in reverse to print the best players first
			for (int j = teamSize - 1; j >= 0; j--)
			{
				CWPlayer player = players.get(ordering[j]);
				output.println("\t\t" + player.realName + " (" + player.username + ")");
			}

		}
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

	private static boolean areTeamsValid(List<CWPlayer> players, List<PlayerRestrictor.PlayerRestriction> restrictions, int[] tempPlayers, int[] teamSizes, double neededAverage, double maxDev)
	{
		int i = 0;
		int teamIndex = 0;
		while (i < tempPlayers.length)
		{
			final int teamSize = teamSizes[teamIndex++];
			double teamStrength = getTeamStrength(players, tempPlayers, i, teamSize);
			//Make sure this team is within range of the max deviation
			if (Math.abs(neededAverage - teamStrength) > maxDev)
			{
				//This team is too good or too bad...
				teamValueFailedCount++;
				return false;
			}
			for (PlayerRestrictor.PlayerRestriction restriction : restrictions)
			{
				if (!restriction.isValidTeam(players, tempPlayers, i, teamSize))
				{
					playerRestrictionsFailedCount++;
					return false;
				}
			}
			i += teamSize;
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
