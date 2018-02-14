import java.util.ArrayList;
import java.util.List;

public class Misc {
    public static List<Integer> mergeSort(List<Integer> unsortedList) {
        int len = unsortedList.size();
        if (len == 1) return unsortedList;

        // sort the left and right sub-lists
        int midIndex = len / 2 ;
        List<Integer> left = mergeSort(unsortedList.subList(0, midIndex));
        List<Integer> right = mergeSort(unsortedList.subList(midIndex, len));

        // merge the two parts
        return merge(left, right);
    }

    public static List<Integer> merge(List<Integer> list1, List<Integer> list2) {
        List<Integer> mergedList = new ArrayList<>();
        int i = 0, j = 0;
        int len1 = list1.size(), len2 = list2.size();
        // two pointers to iterate through both lists together
        // and add the smaller integer to the list
        while (i < len1 && j < len2) {
            int num1 = list1.get(i);
            int num2 = list2.get(j);
            if (num1 < num2) {
                mergedList.add(num1);
                i++;
            } else {
                mergedList.add(num2);
                j++;
            }
        }

        // add the remaining sub-lists
        if (i < len1)
            mergedList.addAll(list1.subList(i, len1));
        if (j < len2)
            mergedList.addAll(list2.subList(j, len2));

        return mergedList;
    }

    // add all integers in the list
    public static double summation(List<Integer> list) {
        double sum = 0;
        for (int i : list)
            sum += i;

        return sum;
    }
}
